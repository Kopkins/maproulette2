# Running Map Roulette Locally
## Install the required software
### PostgreSQL
You will need to install PostgreSQL 9.5 and PosGIS 2.2.1. The default ubuntu repositories for 14.04 do _not_ have the required version of postgreSQL so you will have to [get it elsewhere](http://tecadmin.net/install-postgresql-server-on-ubuntu/). Be sure that you have completely removed any other versions of postgreSQL that you might already have installed. Also be sure to verify the version of postgreSQL is correct after installation.

### Java 8 JDK
You will also need to get a Java 8 JDK which is not in the default Ubuntu repositories. It is easiest to use [another repository](http://ubuntuhandbook.org/index.php/2015/01/install-openjdk-8-ubuntu-14-04-12-04-lts/). Check that your java version is 1.8 `java -version`

## Get the Map Roulette code
You can find the general instructions for installing Map Roulette [here](http://github.com/maproulette/maproulette2) under the Linux heading.

Instead of exporting the database url and the application keys, it is recommended to set them in `dev.conf`. It is located at `conf/dev.conf` in your maproulette2 git repository. By default, the Map Roulette local server uses a dev instance of OpenStreetMap, however you can change this under `osm.server` in `dev.conf`.

Whichever OpenStreetMap server you use, you will need to get your OAuth keys from there. You can do this by selecting the __user dropdown__ in the top right corner, selecting __oauth settings__ at the top of the page, then selecting __register your application__ from the bottom of the page. Fill in the name and use `http://localhost:9000` as the URL. You will then be presented with a page that contains your consumer and secret keys. Fill in the appropriate fields in `dev.conf` with this information.

## Start the server
Running the server is easy. If you've configured your __PATH__ correctly to include activator, you should be able to start the server with `activator run -Dconfig.resource=dev.conf`.

After all the code is compiled you should be able to access your local instance at [localhost:9000](http://localhost:9000).

## Generate some tasks
Build the statistics database and Map Roulette tasks by running `valhalla_build_statistics -c conf/valhalla.json` from your mjolnir directory. By default the GeoJSON is saved to `/data/valhalla/tasks.json`. Once you have the the tasks generated, all you need to do is add them to your instance of Map Roulette.

Open your running instance in your browser and open your projects by selecting __Projects__ from the left hand side navigation menu. Change the visibility of your default project to visible by using the __options__ dropdown on the right and selecting __edit__. Next, click on your project to see the list of challenges associated with it, which is for now empty. Click the __New Challenge__ button on the right to set up your challenge. Fill in the fields how you like and click __next__. Select __Local GeoJSON__ from the navigation bar and then select your generated tasks with the file chooser. You should be able to leave the rest of the setup to default values, then once your tasks are processed, you should be able to view them from either clicking the Map Roulette logo at the top left, or by selecting your challenge from the __latest__ dropdown in the left navigation pane.

Now you should be good to go!
