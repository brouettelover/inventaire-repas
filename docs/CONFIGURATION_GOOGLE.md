# Configurer l'export Google Docs

L'export crée un Google Doc dans ton Drive. Il faut déclarer l'application auprès de Google une fois, dans la Google Cloud Console. Compte une quinzaine de minutes.

L'application demande uniquement la portée `drive.file` : elle ne voit que les fichiers qu'elle crée elle-même, pas le reste de ton Drive.

## 1. Créer un projet

1. Ouvre https://console.cloud.google.com/ avec ton compte Google.
2. En haut à gauche, clique sur le sélecteur de projet, puis **Nouveau projet**.
3. Nom : `Inventaire repas`. Clique sur **Créer**, puis sélectionne ce projet.

## 2. Activer l'API Google Docs

1. Menu ☰ → **API et services** → **Bibliothèque**.
2. Cherche **Google Docs API**, ouvre-la, clique sur **Activer**.

## 3. Écran de consentement OAuth

Selon la version de la console, cette partie s'appelle **Écran de consentement OAuth** ou **Google Auth Platform**.

1. Menu ☰ → **API et services** → **Écran de consentement OAuth** (ou **Google Auth Platform** → **Premiers pas**).
2. **Informations sur l'application** : nom `Inventaire repas`, ton adresse e-mail comme adresse d'assistance.
3. **Audience** : choisis **Externe**.
4. **Coordonnées** : ton adresse e-mail. Accepte les conditions et termine.
5. Dans **Audience** (ou **Utilisateurs tests**), clique sur **Ajouter des utilisateurs** et ajoute ton adresse Gmail. Tant que l'application reste en mode « Test », seuls ces comptes peuvent se connecter, ce qui suffit pour un usage personnel.
6. Dans **Accès aux données** (ou **Champs d'application**), clique sur **Ajouter ou supprimer des champs d'application** et coche `.../auth/drive.file`. Enregistre.

## 4. Récupérer l'empreinte SHA-1 du keystore de debug (Windows)

Android Studio crée automatiquement un keystore de debug dans `%USERPROFILE%\.android\debug.keystore` au premier build.

Le plus simple, depuis le dossier du projet dans PowerShell :

```powershell
.\gradlew.bat signingReport
```

Repère le bloc `Variant: debug` du module `:app` et copie la ligne `SHA1: AA:BB:...`.

Variante avec `keytool` (fourni avec Android Studio) :

```powershell
& "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -list -v `
  -keystore "$env:USERPROFILE\.android\debug.keystore" `
  -alias androiddebugkey -storepass android -keypass android
```

## 5. Créer l'identifiant client Android

1. Menu ☰ → **API et services** → **Identifiants** (ou **Google Auth Platform** → **Clients**).
2. **Créer des identifiants** → **ID client OAuth**.
3. Type d'application : **Android**.
4. Nom du package : `fr.inventairerepas.app`
5. Empreinte SHA-1 : colle celle de l'étape 4.
6. Clique sur **Créer**. Rien à recopier dans le projet : Google reconnaît l'application grâce au package et à la SHA-1.

## 6. Créer l'identifiant client Web

La connexion au compte (Credential Manager) a besoin d'un second identifiant, de type Web.

1. **Créer des identifiants** → **ID client OAuth**.
2. Type d'application : **Application Web**. Nom : `Inventaire repas (connexion)`. Laisse les URI vides.
3. Clique sur **Créer** et copie l'**ID client** (il se termine par `.apps.googleusercontent.com`).

## 7. Renseigner l'identifiant dans le projet

Ouvre le fichier `local.properties` à la racine du projet (Android Studio le crée ; il n'est pas versionné) et ajoute :

```properties
google.webClientId=1234567890-abcdefg.apps.googleusercontent.com
```

Puis recompile et installe :

```powershell
.\gradlew.bat installDebug
```

## 8. Utilisation

1. Dans l'application : **Réglages** → **Se connecter avec Google**, choisis ton compte.
2. Écran **Semaine** → icône de partage → coche si besoin « Inclure le détail des recettes » → **Google Docs**.
3. À la première exportation, Google demande l'autorisation de créer des fichiers dans ton Drive. Accepte.
4. Le document s'appelle « Menu du JJ/MM/AAAA au JJ/MM/AAAA ». Le bouton **Ouvrir** l'affiche.

Sans compte connecté, sans réseau ou en cas d'erreur, l'application propose de partager le même contenu en texte (WhatsApp, e-mail, Keep…).

## Dépannage

| Symptôme | Cause probable |
|---|---|
| Erreur 10 / `DEVELOPER_ERROR` à la connexion | Le package ou la SHA-1 de l'identifiant Android ne correspond pas. Vérifie `fr.inventairerepas.app` et la SHA-1 du keystore réellement utilisé. |
| « Accès bloqué : cette application n'a pas été validée » | Ton adresse n'est pas dans les utilisateurs tests (étape 3.5). |
| « Google Docs API has not been used in project… » (403) | L'API Google Docs n'est pas activée (étape 2), ou l'activation date de moins de quelques minutes. |
| Le bouton « Se connecter » est grisé | `google.webClientId` est absent de `local.properties`, ou le projet n'a pas été recompilé. |
| Ça marche sur un PC et pas sur un autre | Chaque PC a son propre keystore de debug : ajoute la SHA-1 de chaque machine (un identifiant Android par empreinte). |
