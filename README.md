# reactive-rest-mongo

This project implements a rest api pattern using Scala 2.12.4, Play 2.6.18, Scaldi, SMD, ReactiveMongo, and MongoDB. SMD provides built-in Rest API processing so that a minimal number of files need to be added for new api calls. For each domain object that needs to be served up via the api, a new domain case class and object need to be created, a new controller file is created that contains the collection name,  and new routes added to the play routes file. That's it.

This project has been updated to use Play 2.6.18 and Dependency Injection. I have chosen Scaldi over Guice as it is based on implicits and not annotations.

Testing is done via ScalaTest and embedded mongo

The SMD code has been used in production now since June 2015, and was converted into a seperate library in 2018

When I first built this api there was not a lot of documentation on how to do all that I have done, so I wanted to make this available to make someone else's life a bit easier.

If you want to know how it works start with test/controllers/UserSpec. UserSpec tests all the features of the Rest API framework.

Tests can be run using either activator or sbt

To run the tests with activator:
cd reactive-rest-mongo
dberry $ activator
[info] Loading project definition from /Users/dberry/project/exaxis/reactive-rest-mongo/project
[info] Set current project to reactive-rest-mongo (in build file:/Users/dberry/project/exaxis/reactive-rest-mongo/)
[reactive-rest-mongo] $ test

To run the tests with sbt:
cd reactive-rest-mongo
dberry $ sbt
Use of ~/.sbtconfig is deprecated, please migrate global settings to /usr/local/etc/sbtopts
[info] Loading project definition from /Users/dberry/project/exaxis/reactive-rest-mongo/project
[info] Set current project to reactive-rest-mongo (in build file:/Users/dberry/project/exaxis/reactive-rest-mongo/)
[reactive-rest-mongo] $ test

You can also run the project and use curl to exercise the REST API


To set up the mongo db on OSX using brew:

## Install Mongo

  brew install mongodb
  mkdir -p /data/db

## Start Mongo

  mongod --dbpath /data/db





The following curl request will result in an error that lists the available REST endpoints:

  http://localhost:9000/

Example to add a user:

  curl -H "Content-Type: application/json" -X POST -d '{"firstName":"Joe","lastName":"Schmoe", "fullName":"Joe Schmoe", "age": 32, "email":"joe@schmoe.com"}' http://localhost:9000/users

A user _must_ have the following attributes defined:

* firstName: String,
* lastName: String,
* fullName: String,
* age:Option[Int],

A user _may_ define the following attributes:

* id : [String],
* email: [String],
* avatarUrl: [String],
* created : [DateTime],
* updated : [DateTime]

Example to list users:

  http://localhost:9000/users
