#!/usr/bin/env bash

set -euo pipefail

dry=0
VERBOSE="0"
OUT_DIR="./out"
MODE="0"   # -1=quiet, 0=silent, 1=progress, 2=full stats
THREADS="auto"
PLANTUML_OPTS=""

# --- GESTION PARAMÈTRES ---
if [[ "$#" -eq 0 ]] ; then dry=1 ; fi
while [[ "$#" -gt 0 ]] ; do
	case "$1" in
		-v|--verbose) PLANTUML_OPTS="-progress" ; VERBOSE=1 ;;
		--verbose-debug)
			PLANTUML_OPTS="-progress -enablestats -realtimestats -duration -verbose"
			VERBOSE=2
			;;
		--quiet) PLANTUML_OPTS="-quiet" ; VERBOSE=0 ;;
		-o|--output) shift; OUT_DIR="$1" ;;
		-j|--nb-thread) shift; THREADS="$1" ;;
		--dry) dry="1" ;;
		--run) dry="0" ;;
	esac
	shift
done

# --- CHECK DEPENDANCES ---
command -v plantuml >/dev/null 2>&1 || { echo "plantuml manquant"; exit 1; }
command -v inkscape >/dev/null 2>&1 || { echo "inkscape manquant"; exit 1; }

# --- CONVERT ---
svg2pdf(){
	local svg="$1"
	local pdf="${1%.svg}.pdf"
	local more=""
	if [[ "$VERBOSE" -ge 2 ]] ; then
		more="$2) "
		echo "$more""Convert '$svg' to '$pdf'"
	fi
	inkscape "$svg" --export-type=pdf --export-filename="$pdf"
	local ret=$?
	if [[ $ret -ne 0 ]] ; then
		echo "Impossible de convertir $svg en pdf."
		return $?
	fi
	if [[ "$VERBOSE" -ge 1 ]] ; then
		echo "$more'$svg' -> '$pdf'"
	fi
	rm -f "$svg"
	return 0
}

# --- PREP ---
mkdir -p "$OUT_DIR"
opts=( -tsvg -o "$OUT_DIR" -nbthread "$THREADS" $PLANTUML_OPTS "**/*.puml" )
svg2pdfOpts=( "$OUT_DIR" -type f -name "*.svg" -print)

# --- EXEC ---

if [[ "$dry" -eq 0 ]] ; then
	plantuml "${opts[@]}"
	i=0
	for f in $(find "${svg2pdfOpts[@]}") ; do
		i="$(($i + 1))"
		svg2pdf "$f" "$i" &
		sleep 1
	done
else
	echo "[dry] plantuml $(for opt in "${opts[@]}" ; do echo -n " '$opt'" ; done)"
	echo "[dry] for f in \$(find $(for opt in "${svg2pdfOpts[@]}" ; do echo -n " '$opt'" ; done)) ; do svg2pdf \"\$f\" & done"
fi

wait

echo "END CONVERT *.puml -> *.pdf"
if [[ "$VERBOSE" -ge 1 ]] ; then
	if [[ "$VERBOSE" -ge 2 ]] ; then
		ls .
	fi
	ls "$OUT_DIR"
fi
if [[ "$VERBOSE" -ge 1 ]] ; then
	ls "$OUT_DIR"
fi

exit 0
