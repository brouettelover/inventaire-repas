# Inventaire repas

Application Android personnelle pour planifier les repas de la semaine, générer les listes de courses, gérer le stock et consulter les recettes. Hors ligne, mono-utilisateur.

## Stack

Kotlin, Jetpack Compose (Material 3), MVVM, Room, Hilt, Navigation Compose. minSdk 26.

## Modules

- `:domain` : logique métier en Kotlin pur, testable sans Android.
- `:data` : Room, DataStore, dépôts, seed, export Google Docs.
- `:app` : interface Compose.

## Commandes (PowerShell)

```powershell
.\gradlew.bat :domain:test     # tests unitaires du domaine
.\gradlew.bat assembleDebug    # APK de debug
.\gradlew.bat installDebug     # installation sur l'appareil ou l'émulateur
```

## Export Google Docs

L'export crée un Google Doc avec le menu de la semaine et les deux listes de courses, et en option le détail des recettes. La configuration côté Google Cloud Console est décrite pas à pas dans [docs/CONFIGURATION_GOOGLE.md](docs/CONFIGURATION_GOOGLE.md). Sans configuration ni compte connecté, l'application propose un partage en texte. Le document peut être partagé automatiquement avec une seconde adresse (Réglages → Partage).

## Recettes sur Google Drive

Les recettes sont stockées dans un dossier `recettes` de Google Drive, un Google Doc par recette, et synchronisées dans les deux sens : à l'ouverture de l'app, toutes les 5 minutes, après chaque modification et avec le bouton ⟳. Modifier, ajouter ou supprimer un document dans ce dossier met l'app à jour. Le dossier peut être partagé avec une seconde personne. Format et mode d'emploi : section 7 de [docs/CONFIGURATION_GOOGLE.md](docs/CONFIGURATION_GOOGLE.md).
