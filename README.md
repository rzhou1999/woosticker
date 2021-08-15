# THIS PROJECT IS ON INDEFINITE HIATUS

I currently have neither the time nor interest to further develop this app. Please visit https://github.com/FredHappyface/Android.EweSticker for a more actively maintained fork of this project. Comments and PRs will not be answered.

# woosticker

It's like [uSticker](https://github.com/apsun/uSticker) except with exceptionally worse programming. Code is not anywhere near publishable state, but app is functional at the least so I thought I would share.

The main difference between this app and uSticker is that uSticker piggybacks on top of Gboard's custom sticker functionality. Unfortunately, that feature has been removed as of Gboard version 10.3. woosticker instead implements a new keyboard altogether and uses the [Commit Content API](https://developer.android.com/guide/topics/text/image-keyboard) to send images.

I am currently releasing woosticker through [GitHub releases](https://github.com/rzhou1999/woosticker/releases) only-- I'd like to get to a point where I'm happy with the UI before I release to play store/fdroid. As such, you will likely get an installation warning when downloading and installing, which you will need to dismiss in order to use woosticker. You can also build the project manually to generate an .apk as well.

Note that this is primarily a personal project, and the feature set will largely be determined by my own needs. I am happy to hear feedback (submit an Issue!) about the user experience and use-cases, but please understand if I cannot accomodate feature requests. If you feel strongly about a specific issue, please feel free to fork this project!

## Planned to-dos:

* [x] ~~Move file transfer to separate thread~~
* [ ] Find better way to reload from filesystem (how to trigger onCreate() programmatically?)
* [ ] UI improvements + tutorial
* [ ] Add more than just png and gif support
* [ ] Clean up code + add javadoc documentation
* [ ] If there's something else you would like to see please submit an issue (no guarantees on when I'll get around to doing them however)

## Sticker-use disclaimer

woosticker was intended to provide pseudo-sticker support for messaging apps that don't natively have their own implementation of stickers-- for example, Threema, XMPP, etc. allow text and image support, but do not have a native sticker store or the like. Unfortunately, this also means that there is no direct way to support artists and creators directly from woosticker itself, since the app is intended to treat images/folders and stickers/packs without any extra metadata and ultimately raises questions about the usage of stickers created by others. If you do use others' artwork, I highly encourage anyone who uses the artwork of others as a sticker to purchase the relevant pack on existing sticker stores (such as LINE's sticker storefront) or in another way monetarily support the artists. **I do not condone the usage of woosticker to sidestep the need to pay for an app's existing sticker packs/features.**

## Screenshots

Current landing screen (it's hideous, I know):

![image1](https://raw.githubusercontent.com/rzhou1999/woosticker/main/screenshots/1.png)

Keyboard layout:

![image2](https://raw.githubusercontent.com/rzhou1999/woosticker/main/screenshots/2.png)

Example file structure:

![image3](https://raw.githubusercontent.com/rzhou1999/woosticker/main/screenshots/3.png)

All individual directories are put into their own "pack." The base directory also can constitute its own pack of there are image files directly inside of it.
