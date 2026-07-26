# Vendetta System — How to get your .jar (no PC setup needed)

This project builds itself automatically in the cloud using GitHub Actions.
You don't need to install Java, Maven, or anything else on your computer.

## Steps

1. Go to https://github.com/new and create a **new repository** (any name, e.g. `vendetta-system`). Keep it Public or Private, either works.
2. On your new repo's page, click **"uploading an existing file"** (or the "Add file" → "Upload files" button).
3. Drag this **entire folder's contents** (including the hidden `.github` folder) into the upload box. If your browser hides the `.github` folder, unzip normally, show hidden files, and drag it in separately — it must exist for the auto-build to work.
4. Click **Commit changes**.
5. Go to the **"Actions"** tab at the top of your repo. You'll see a build running (takes ~1-2 minutes).
6. Once it finishes with a green checkmark, click into it, scroll down to **"Artifacts"**, and download **VendettaSystem-jar**. Unzip that download — inside is `VendettaSystem.jar`.
7. Drop `VendettaSystem.jar` into your Paper server's `/plugins` folder and restart the server.

## Requirements on your server
- Paper (or Fork) for Minecraft 1.21.x
- Vault plugin (for the money/economy features)
- An economy plugin Vault can hook into (e.g. EssentialsX)
- Geyser + Floodgate (for Bedrock players)

## Config
After first launch, edit `plugins/VendettaSystem/config.yml` to adjust thresholds, bounty minimums, hardcore lives, and Reckoning schedule.

## Note on The Reckoning
The arena world (`reckoning_arena` by default) must already exist on your server — create/import that world and set the name in config.yml to match.
