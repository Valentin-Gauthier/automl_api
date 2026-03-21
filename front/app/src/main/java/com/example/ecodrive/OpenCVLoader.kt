package com.example.ecodrive

import android.os.CountDownTimer
import android.util.Log
import org.opencv.android.OpenCVLoader
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock

object OpenCVLoader {
    private const val TAG = "OpenCVLoaderManager"

    // États possibles
    private enum class OpenCVState {
        NOT_INITIALIZED,
        INITIALIZING,
        LOADED,
        FAILED
    }

    // État actuel
    private var state = OpenCVState.NOT_INITIALIZED
    private var echecCounter = 0

    // Verrou et condition pour la synchronisation
    private val lock = ReentrantLock()
    private val condition: Condition = lock.newCondition()

    /**
     * Initialise OpenCV dans un thread séparé.
     * Cette méthode peut être appelée dans un thread avec `thread { OpenCVLoaderManager.init() }`.
     * @return `true` si OpenCV est chargé avec succès, `false` sinon.
     */
    fun init(): Boolean {
        lock.lock()
        try {
            when (state) {
                OpenCVState.LOADED -> return true// Already load => silent success
                OpenCVState.INITIALIZING -> {
                    // Already in loading => wait end of the loading
                    condition.await()
                    return state == OpenCVState.LOADED
                }
                OpenCVState.FAILED -> {
                    // Have already being loading, but the precedent try have failed.
                    // => alert the user of the new try for load OpenCV
                    Log.d(TAG, "Nouvelle tentative (${echecCounter}e) de chargement d'OpenCV après un échec précédent.")
                }
                OpenCVState.NOT_INITIALIZED -> {
                    // Haven't tried to load. Initial state. The normal state for this function.
                    // => Nothing
                }
            }
            state = OpenCVState.INITIALIZING
        } finally {
            lock.unlock()
        }

        // Tentative de chargement d'OpenCV
        val isLoaded = OpenCVLoader.initLocal()

        lock.lock()
        try {
            if (isLoaded) {
                state = OpenCVState.LOADED
                // Silent success
            } else {
                echecCounter++
                Log.e(TAG, "${echecCounter}e Échec du chargement d'OpenCV.")
                state = OpenCVState.FAILED
            }
            // Libère tous les threads en attente
            condition.signalAll()
            return isLoaded
        } finally {
            lock.unlock()
        }
    }

    /**
     * Attend que OpenCV soit chargé, avec un nombre maximal de tentatives et un timeout par essai.
     * @param maxRetries Nombre maximal de tentatives de rechargement.
     * @param millisInFuture Temps maximal total pour attendre (en ms). Si `null`, pas de timeout.
     * @param countDownInterval The interval along the way to receive onTick(long) callbacks. Si 'null', pas de onTick(long) callbacks.
     * @param onTick Callback appelé tous les countDownInterval. Permet de mettre en place une barre de chargement.
     * @return `true` si OpenCV est chargé, `false` sinon.
     */
    fun waitOpenCV(
        maxRetries: Int = 1,
        millisInFuture: Long? = null,
        countDownInterval: Long?  = null,
        onTick: ((Long) -> Unit)? = null
    ): Boolean {
        // Set alarm for respect limit of time to try
        var timeout: CountDownTimer? = null
        var isTimedOut = false
        millisInFuture?.let {
            val cdi = countDownInterval ?: (millisInFuture + 10)
            val ot: ((Long) -> Unit) = onTick ?: fun(a: Long){}
            timeout = object : CountDownTimer(millisInFuture, cdi) {
                override fun onTick(millisUntilFinished: Long) {
                    ot.invoke(millisUntilFinished)
                }
                override fun onFinish() {
                    isTimedOut = true
                }
            }.start()
        }
        try {
            // First try is a free try
            lock.lock()
            try {
                if (state == OpenCVState.NOT_INITIALIZED) {
                    Log.e(TAG, "init wasn't call, I try to load OpenCV in late.")
                    val success = init()
                    if (success)
                        return true
                } else if (state == OpenCVState.LOADED) {
                    // Silent success
                    return true
                }
            } finally {
                lock.unlock()
            }
            // Retries to load
            var retries = 0

            while (!isTimedOut && retries < maxRetries) {
                val success = init()
                if (success)
                    return true
                retries++
            }
            Log.e(TAG, "Max retry has been touch")
        } catch (e: InterruptedException) {
            Log.e(TAG, "Max time has been touch: ${e.message}")
        } finally {
            timeout?.let {timeout.cancel()}
        }
        return false
    }
}