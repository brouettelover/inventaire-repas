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

## Recettes partagées

L'écran Recettes exporte et importe les recettes dans un fichier JSON (par exemple sur Google Drive), et le bouton ⟳ synchronise ce fichier entre deux téléphones : la version modifiée le plus récemment gagne, les suppressions ne sont pas propagées. Mode d'emploi : section 9 de [docs/CONFIGURATION_GOOGLE.md](docs/CONFIGURATION_GOOGLE.md).

