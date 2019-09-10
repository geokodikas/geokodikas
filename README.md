Geokodikas [![CircleCI](https://circleci.com/gh/geokodikas/geokodikas/tree/master.svg?style=svg)](https://circleci.com/gh/geokodikas/geokodikas/tree/master)
==========

## What is this?

Geokodikas is a new geocoder using OpenStreetMap as main data source.
The focus currently is on reverse geocoding while we also want to implement forward geocoding.

## Why another geocoder?

The OpenStreetMap community uses [Nominatim](https://github.com/openstreetmap/nominatim) as default geocoder.
This is a great project and it has proven itself, e.g., it is used for powering the search on openstreetmaps.org
However, it also has some shortcomings (which we later will document in more detail):

 - API isn't flexible enough for our use cases, Overpass Turbo is of course very flexible but at same is much slower than Nominatim
   For example, getting the speed limit of a highway in the same request as reverse geocoding.
 - long import times
 - Sometimes the address returned by Nominatim isn't what we expect it to be.
   For example searching for `Inuits` gives `Inuits, 31, Essensteenweg, KMO-zone Bosduin, Kapellen, Brasschaat, Antwerp, Flanders, 2930, Belgium`.
   The response both contains `Kapellen` and `Brasschaat`, where we would expect it to return only `Brasschaat`.
   Furthermore, the address it is not located in the `KMO-ZOne Bosduin`, but this is returned in the address.

We know the value of Nominatim and certainly don't want to criticise it.
This project is still very much Work in Progress, don't expect it to outperform Nominatim right now.
For example, currently we only test in Belgium, some things must be changed before other or multi region will work.
We will elaborate more about other geocoders in the future.


## Feedback

If you have an idea or see something which can be improved don't hesitate to open an issue in order to discuss it.
This also holds for questions.


## In cooperation with

Inuits

TBA

## Used Technologies

TBA

## License

GPLv3