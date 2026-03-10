# Images des véhicules (PNG)

```
storage interne + chemin dans la DB
```




# Logos SVG

**Il faut les convertir**.

Android ne supporte pas directement SVG dans les layouts.
Il faut les importer en **VectorDrawable (XML)**.

### Comment faire

Dans **Android Studio :**

```
res
→ drawable
→ clic droit
→ New
→ Vector Asset
```

Puis :

```
Asset Type → Local File (SVG)
```

Importer :

```
logo_ecodrive.svg
home.svg
add_vehicle.svg
search_vehicle.svg
```

Android va générer :

```
res/drawable/logo_ecodrive.xml
res/drawable/ic_home.xml
res/drawable/ic_add_vehicle.xml
res/drawable/ic_search.xml
```

C'est **la méthode recommandée par Google**.




# Page d'accueil

L’idée est :
* **Un layout réutilisable pour le bandeau supérieur**
* **Un layout réutilisable pour le bandeau inférieur**
* **Une activité d’accueil**
* **Une base de données interne (Room recommandé)**
* **Une galerie avec RecyclerView**
