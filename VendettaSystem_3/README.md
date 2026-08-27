# Vendetta System — How to get your .jar (no PC setup needed)

This project builds itself automatically in the cloud using GitHub Actions.
You don't need to install Java, Maven, or anything else on your computer.

## Steps

1. Go to https://github.com/new and create a **new repository**.
2. Delete any old files in the repo first if you're reusing one (important — stale files from a previous version can break the build).
3. Click **"Add file" → "Upload files"**.
4. Drag in **every file and folder from this project** (including the hidden `.github` folder — if your file browser hides it, enable "show hidden files" first).
5. Make sure `pom.xml` ends up at the **root** of the repo, not nested inside a subfolder.
6. Click **Commit changes**.
7. Go to the **"Actions"** tab. A build will start automatically — wait for the green checkmark (~1-2 minutes).
8. Click into the finished run, scroll to **"Artifacts"**, download **VendettaSystem-jar**, and unzip it. Inside is `VendettaSystem.jar`.
9. Drop that jar into your Paper server's `/plugins` folder and restart.

## Requirements on your server
- Paper (or fork) for Minecraft 1.21.x
- Geyser + Floodgate (for Bedrock players)
- No economy plugin needed — this version has no money-based features.

## Config
After first launch, edit `plugins/VendettaSystem/config.yml` to adjust curse timings, starting lives, and death-curse thresholds.
