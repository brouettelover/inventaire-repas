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
