# woosticker

It's like [uSticker](https://github.com/apsun/uSticker) except with exceptionally worse programming. Code is not anywhere near publishable state, but app is functional at the least so I thought I would share.

The main difference between this app and uSticker is that uSticker piggybacks on top of Gboard's custom sticker functionality. Unfortunately, that feature has been removed as of Gboard version 10.3. woosticker instead implements a new keyboard altogether and uses the [Commit Content API](https://developer.android.com/guide/topics/text/image-keyboard) to send images.

I am currently releasing woosticker through [GitHub releases](https://github.com/rzhou1999/woosticker/releases) only-- I'd like to get to a point where I'm happy with the UI before I release to play store/fdroid. As such, you will likely get an installation warning when downloading and installing, which you will need to dismiss in order to use woosticker. You can also build the project manually to generate an .apk as well.

## Planned to-dos:

* [x] ~~ Move file transfer to separate thread ~~
* [ ] Find better way to reload from filesystem (how to trigger onCreate() programmatically?)
* [ ] UI improvements + tutorial
* [ ] Add more than just png and gif support
* [ ]Clean up code + add javadoc documentation
* If there's something else you would like to see please submit an issue (no guarantees on when I'll get around to doing them however)


## Screenshots

Current landing screen (it's hideous, I know):

![image1](https://raw.githubusercontent.com/rzhou1999/woosticker/main/screenshots/1.png)

Keyboard layout:

![image2](https://raw.githubusercontent.com/rzhou1999/woosticker/main/screenshots/2.png)

Example file structure:

![image3](https://raw.githubusercontent.com/rzhou1999/woosticker/main/screenshots/3.png)

All individual directories are put into their own "pack." The base directory also can constitute its own pack of there are image files directly inside of it.
