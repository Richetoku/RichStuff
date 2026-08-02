@echo off
echo Pushing RichStuff to GitHub...

cd RichStuff
git remote set-url origin https://github.com/Richetoku/RichStuff.git
git add .
git commit -m "Initial commit: Recipe fixes and improvements

- Fixed 56 rod recipes (2x2 corner pattern)
- Fixed 56 wire recipes (shears+plate)
- Added 168 wire recipes (Create, Vintage, Vanilla)
- Added 4 bitumen recipes
- Created RecipeLoader.java for recipe priority
- Added comprehensive documentation" --allow-empty
git push -u origin main

echo RichStuff push complete!