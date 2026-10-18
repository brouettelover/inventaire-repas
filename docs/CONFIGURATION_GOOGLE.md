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
3. Reviens à la bibliothèque, cherche **Google Drive API** et active-la aussi. Elle sert à partager le document créé avec une autre personne.

## 3. Écran de consentement OAuth

Selon la version de la console, cette partie s'appelle **Écran de consentement OAuth** ou **Google Auth Platform**.

1. Menu ☰ → **API et services** → **Écran de consentement OAuth** (ou **Google Auth Platform** → **Premiers pas**).
2. **Informations sur l'application** : nom `Inventaire repas`, ton adresse e-mail comme adresse d'assistance.
3. **Audience** : choisis **Externe**.
4. **Coordonnées** : ton adresse e-mail. Accepte les conditions et termine.
5. Dans **Audience** (ou **Utilisateurs tests**), clique sur **Ajouter des utilisateurs** et ajoute ton adresse Gmail, ainsi que celle de ta copine si elle exporte aussi depuis son téléphone. Tant que l'application reste en mode « Test », seuls ces comptes peuvent se connecter, ce qui suffit pour un usage personnel.
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
6. Clique sur **Créer**. Rien à recopier dans le projet ni dans l'application : Google reconnaît l'application grâce au package et à la SHA-1. Aucun identifiant de type « Application Web » n'est nécessaire.

## 6. Utilisation

1. Dans l'application : **Réglages** → **Se connecter avec Google**, choisis ton compte et accepte l'accès demandé (création de fichiers dans ton Drive).
2. Écran **Semaine** → icône de partage → coche si besoin « Inclure le détail des recettes » → **Google Docs**.
3. Le document s'appelle « Menu du JJ/MM/AAAA au JJ/MM/AAAA ». Le bouton **Ouvrir** l'affiche.

Sans compte connecté, sans réseau ou en cas d'erreur, l'application propose de partager le même contenu en texte (WhatsApp, e-mail, Keep…).

### Partager le Google Doc avec une autre personne

1. **Réglages** → **Partage** : saisis son adresse Gmail, puis **Enregistrer**.
2. À chaque export, la case « Partager le Google Doc avec … » est cochée par défaut. Elle reçoit un e-mail de Google avec le lien et peut modifier le document (cocher les courses, par exemple).
3. Si le partage échoue, le document est quand même créé et l'application l'indique. Tu peux alors le partager à la main depuis Google Docs.

## 7. Recettes partagées entre deux téléphones

Les recettes sont échangées par un fichier `recettes-inventaire-repas.json` posé sur Google Drive. Aucune configuration de la Cloud Console n'est nécessaire : le fichier passe par le sélecteur de fichiers d'Android, comme une pièce jointe.

**Une seule fois, sur ton téléphone :**

1. Écran **Recettes** → menu ⋮ → **Exporter vers un fichier…**
2. Dans le sélecteur, ouvre le menu ☰ et choisis **Drive** (ton compte), puis **Enregistrer**.
3. Dans l'application Google Drive, partage ce fichier avec ta copine en tant qu'**Éditeur**.

**Une seule fois, sur son téléphone :**

1. Dans l'application Google Drive, ouvre **Partagés avec moi**, appuie sur ⋮ à côté du fichier → **Ajouter un raccourci dans Drive** → **Mon Drive**. Le sélecteur de fichiers d'Android n'affiche pas toujours les fichiers partagés ; le raccourci le rend visible.
2. Dans l'application : **Recettes** → ⋮ → **Importer un fichier…** → **Drive** → le fichier.

**Ensuite, sur les deux téléphones :** l'icône ⟳ de l'écran Recettes synchronise. Elle lit le fichier, ajoute ou met à jour les recettes, puis réécrit le fichier avec le résultat, pour que l'autre téléphone récupère tes changements à sa prochaine synchronisation.

Règles de fusion :

- Les recettes sont reconnues par leur nom (sans tenir compte des majuscules et des accents).
- Quand une recette existe des deux côtés, la version modifiée le plus récemment gagne.
- Supprimer une recette sur un téléphone ne la supprime pas sur l'autre : elle reviendrait à la synchronisation suivante. Supprime-la sur les deux téléphones, puis synchronise.
- Le menu, le stock et les listes de courses restent propres à chaque téléphone.

Drive met parfois quelques secondes à envoyer le fichier : si une modification de l'autre téléphone n'apparaît pas, attends un peu et synchronise à nouveau. Les deux téléphones doivent avoir une heure correcte, puisque c'est la date de modification qui départage.

Le même fichier sert aussi de sauvegarde : tes recettes sont dans Drive même si tu désinstalles l'application.

## Dépannage

| Symptôme | Cause probable |
|---|---|
| Erreur 10 / `DEVELOPER_ERROR`, ou « Connexion impossible » | Le package ou la SHA-1 de l'identifiant Android ne correspond pas. Vérifie `fr.inventairerepas.app` et la SHA-1 du keystore réellement utilisé. |
| « Accès bloqué : cette application n'a pas été validée » | Ton adresse n'est pas dans les utilisateurs tests (étape 3.5). |
| « Google Docs API has not been used in project… » (403) | L'API Google Docs n'est pas activée (étape 2), ou l'activation date de moins de quelques minutes. |
| « Google Drive API has not been used in project… » (403) au partage | L'API Google Drive n'est pas activée (étape 2.3). |
| « Ce fichier est en lecture seule » à la synchronisation | Le fichier Drive est partagé en lecture seule : donne le rôle **Éditeur**. |
| Le fichier partagé n'apparaît pas dans le sélecteur | Ajoute un raccourci dans **Mon Drive** depuis l'application Google Drive (étape 7). |
| Ça marche sur un PC et pas sur un autre | Chaque PC a son propre keystore de debug : ajoute la SHA-1 de chaque machine (un identifiant Android par empreinte). |
