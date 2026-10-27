# Configurer Google Drive et Google Docs

L'application utilise ton Google Drive pour deux choses : stocker les recettes dans un dossier « recettes » synchronisé, et exporter le menu de la semaine dans un Google Doc. Il faut déclarer l'application auprès de Google une fois, dans la Google Cloud Console. Compte une quinzaine de minutes.

L'application demande l'accès à ton Drive (portée `drive`) : c'est nécessaire pour lire les recettes que tu ajoutes toi-même dans le dossier, ou que ta copine y ajoute. Elle ne touche qu'au dossier « recettes » et aux documents qu'elle exporte.

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
6. Dans **Accès aux données** (ou **Champs d'application**), clique sur **Ajouter ou supprimer des champs d'application** et coche `.../auth/drive` (« Afficher, modifier, créer et supprimer tous vos fichiers Google Drive »). Si elle n'apparaît pas dans la liste, colle `https://www.googleapis.com/auth/drive` dans **Ajouter manuellement des champs d'application**. Enregistre. Google la classe « restreinte » : c'est sans conséquence tant que l'application reste en mode « Test » avec vos deux adresses en utilisateurs tests.

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
3. Google demande ensuite l'accès à ton Drive. S'il ne le fait pas tout de suite, un bandeau « Autoriser » apparaît sur l'écran **Recettes**.
4. Le document s'appelle « Menu du JJ/MM/AAAA au JJ/MM/AAAA ». Le bouton **Ouvrir** l'affiche.

Sans compte connecté, sans réseau ou en cas d'erreur, l'application propose de partager le même contenu en texte (WhatsApp, e-mail, Keep…).

### Partager le Google Doc avec une autre personne

1. **Réglages** → **Partage** : saisis son adresse Gmail, puis **Enregistrer**.
2. À chaque export, la case « Partager le Google Doc avec … » est cochée par défaut. Elle reçoit un e-mail de Google avec le lien et peut modifier le document (cocher les courses, par exemple).
3. Si le partage échoue, le document est quand même créé et l'application l'indique. Tu peux alors le partager à la main depuis Google Docs.

## 7. Recettes dans le dossier Drive « recettes »

Les recettes vivent dans un dossier `recettes` de ton Google Drive, un Google Doc par recette. Le nom du document est le nom de la recette. L'application se synchronise toute seule :

- à l'ouverture de l'application, puis toutes les 5 minutes tant qu'elle est affichée ;
- juste après la création, la modification ou la suppression d'une recette dans l'application ;
- quand tu appuies sur ⟳ en haut de l'écran **Recettes** (ou **Réglages** → **Synchroniser**).

**Première synchronisation.** Après la connexion à Google, l'application cherche un dossier nommé `recettes` dans ton Drive et le crée s'il n'existe pas. Elle y envoie toutes tes recettes.

**Modifier une recette depuis Drive.** Ouvre le document dans Google Docs (sur le téléphone ou le PC) et modifie-le. Le changement arrive dans l'application à la synchronisation suivante. Garde la structure ci-dessous ; la mise en forme (gras, titres, puces) n'a pas d'importance.

```
Portions : 2
Préparation : 15 min
Cuisson : 1 h 30
Fraîcheur : J+1
Saison : octobre à février

Ingrédients
- 200 g pâtes
- 1 c. à soupe huile d'olive
- 2 œufs

Étapes
1. Faire cuire les pâtes.
2. Mélanger et servir.
```

- **Fraîcheur** : J+0, J+1 ou J+2. **Saison** : `toute l'année`, une liste (`janvier, mars`) ou une période (`octobre à février`).
- **Unités** comprises : g, kg, ml, cl, l, c. à café, c. à soupe, pièce. Sans unité (`2 œufs`), c'est un nombre de pièces. Sans quantité (`sel`), c'est 1 pièce.
- Les lignes absentes prennent une valeur par défaut (2 portions, 0 min, J+0, toute l'année).

**Ajouter une recette depuis Drive.** Crée un nouveau Google Doc dans le dossier `recettes`, nomme-le comme la recette, et écris-la dans le format ci-dessus. Un fichier texte `.txt` fonctionne aussi.

**Supprimer.** Mettre un document à la corbeille supprime la recette dans l'application, et inversement. Si une recette a été modifiée d'un côté pendant qu'elle était supprimée de l'autre, la modification l'emporte et la recette revient.

**Conflits.** Si une recette est modifiée des deux côtés entre deux synchronisations, la version la plus récente gagne.

**Fichier illisible.** Si un document ne respecte pas le format (durée ou mois incompris, par exemple), il est ignoré et un bandeau sur l'écran **Recettes** indique le fichier et la ligne à corriger. Les autres recettes sont synchronisées normalement.

### Partager les recettes avec ta copine

1. Sur ton téléphone, laisse l'application créer le dossier `recettes` (première synchronisation).
2. Dans Google Drive, partage le dossier `recettes` avec son adresse Gmail, en tant qu'**Éditeur**.
3. Ajoute son adresse aux utilisateurs tests (étape 3.5).
4. Sur son téléphone, installe l'application et connecte-toi avec son compte dans **Réglages**. L'application trouve le dossier partagé (le plus ancien dossier nommé `recettes` auquel elle a accès) et récupère les recettes.

Si son téléphone s'est synchronisé avant le partage, il a créé son propre dossier : supprime-le dans son Drive, puis **Réglages** → **Rechercher le dossier**.

Le menu, le stock et les listes de courses restent propres à chaque téléphone.

### Sauvegarde ponctuelle

Le menu ⋮ de l'écran **Recettes** permet aussi d'exporter toutes les recettes dans un fichier JSON, et de réimporter ce fichier (par exemple pour transférer les recettes sans compte Google). À l'import, les recettes sont rapprochées par leur nom et la version la plus récente gagne.

## Dépannage

| Symptôme | Cause probable |
|---|---|
| Erreur 10 / `DEVELOPER_ERROR`, ou « Connexion impossible » | Le package ou la SHA-1 de l'identifiant Android ne correspond pas. Vérifie `fr.inventairerepas.app` et la SHA-1 du keystore réellement utilisé. |
| « Accès bloqué : cette application n'a pas été validée » | Ton adresse n'est pas dans les utilisateurs tests (étape 3.5). |
| « Google Docs API has not been used in project… » (403) | L'API Google Docs n'est pas activée (étape 2), ou l'activation date de moins de quelques minutes. |
| « Accès à Google Drive refusé » (403) à la synchronisation | L'API Google Drive n'est pas activée (étape 2.3), ou la portée `.../auth/drive` n'est pas ajoutée (étape 3.6). Après correction, appuie sur ⟳ ; si Google redemande l'accès, accepte. |
| Bandeau « Autorise l'accès à ton dossier Drive » | L'application a besoin d'un nouvel accès (par exemple après une mise à jour) : appuie sur **Autoriser**. |
| Ta copine ne voit pas tes recettes | Le dossier n'est pas partagé avec elle en **Éditeur**, ou son téléphone utilise son propre dossier : voir « Partager les recettes avec ta copine ». |
| Une recette modifiée dans Drive n'arrive pas | Appuie sur ⟳. Si un bandeau signale le fichier, corrige la ligne indiquée. |
| Ça marche sur un PC et pas sur un autre | Chaque PC a son propre keystore de debug : ajoute la SHA-1 de chaque machine (un identifiant Android par empreinte). |
