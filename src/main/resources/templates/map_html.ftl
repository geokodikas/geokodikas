<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/css/bootstrap.min.css"
          integrity="sha384-Gn5384xqQ1aoWXA+058RXPxPg6fy4IWvTNh0E263XmFcJlSAwiGgFAW/dAiS6JXm" crossorigin="anonymous">
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.5.1/dist/leaflet.css"
          integrity="sha512-xwE/Az9zrjBIphAcBb3F6JVqxf46+CDLwfLMHloNu6KEQCAWi6HcDUbeOfBIptF7tcCzusKFjFw2yuvEpDL9wQ=="
          crossorigin=""/>
    <!-- Make sure you put this AFTER Leaflet's CSS -->
    <script src="https://unpkg.com/leaflet@1.5.1/dist/leaflet.js"
            integrity="sha512-GffPMF3RvMeYyc1LWMHtK8EbPv0iNZ8/oTtHPx9/cc2ILxQ+u905qIwdpULaqDkyBKgOaB57QTMg7ztg8Jm2Og=="
            crossorigin=""></script>
    <style>
        #mapid {
            width: 100%;
            height: 100%;
        }

        .map-column {
            height: 100vh;
        }

        .main-row {
            margin: auto;
        }

        pre {
            white-space: pre-wrap;
            word-wrap: break-word;
        }

        .details-top, .details-bottom {
            height: 50vh;
        }

        table {
            margin-bottom: 0;
        }
    </style>
</head>
<body>
<div class="fluid-container">
    <div class="row main-row">
        <div class="col-6 map-column">
            <div id="mapid"></div>
        </div>
        <div class="col-6">
            <br>
            <details>
                <pre><code>${geojson}</code></pre>
            </details>
            <br>
            ${tabs}
        </div>
    </div>

</div>


<script src="https://code.jquery.com/jquery-3.3.1.slim.min.js"
        integrity="sha384-q8i/X+965DzO0rT7abK41JStQIAqVgRVzpbzo5smXKp4YfRvH+8abtTE1Pi6jizo"
        crossorigin="anonymous"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.14.7/umd/popper.min.js"
        integrity="sha384-UO2eT0CpHqdSJQ6hJty5KVphtPhzWj9WO1clHTMGa3JDZwrnQq4sF86dIHNDz0W1"
        crossorigin="anonymous"></script>
<script src="https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/js/bootstrap.min.js"
        integrity="sha384-JjSmVgyd0p3pXB1rRibZUAYoIIy6OrQ6VrjIEaFf/nJGzIxFDsf4x0xIM+B07jRM"
        crossorigin="anonymous"></script>
<script>
    let geojsonFeature = ${geojson};

    let mymap = L.map('mapid');

    let geojsonMarkerOptions = {
        radius: 8,
        fillColor: "#3388ff",
        color: "#3388ff",
        weight: 1,
        opacity: 1,
        fillOpacity: 0.8
    };

    let group = L.geoJSON(geojsonFeature, {
        onEachFeature: onEachFeature,
        pointToLayer: function (feature, latlng) {
            return L.circleMarker(latlng, geojsonMarkerOptions);
        }
    }).addTo(mymap);

    function onEachFeature(feature, layer) {
        if (layer.feature.id === "input-point") {
            layer.setStyle({fillColor: 'green', color: 'green'})
        } else {
            layer.on({
                click: function () {
                    $('#tab-btn-' + layer.feature.properties.osm_id).tab('show');
                }
            });
        }
    }

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: 'Map data © <a href="https://openstreetmap.org">OpenStreetMap</a> contributors',
        minZoom: 8,
        maxZoom: 18
    }).addTo(mymap);

    mymap.fitBounds(group.getBounds());

    let firstLayer = group.getLayers().filter(el => (el.feature.id !== "input-point") && el.feature.id !== "closest-point" )[0];
    $('#tab-btn-' + firstLayer.feature.properties.osm_id).tab('show');
    resetHighlighting();
    highlightFeature(firstLayer.feature.properties.osm_id);

    function resetHighlighting() {
        group.eachLayer(function (layer) {
            if (layer.feature.id === "input-point") {
                layer.setStyle({fillColor: 'green', color: 'green'})
            } else if (layer.feature.id === "closest-point") {
                layer.setStyle({fillColor: 'orange', color: 'orange'})
            } else {
                layer.setStyle({fillColor: '#3388ff', color: '#3388ff'})
            }
        });
    }

    function highlightFeature(featureId) {
        resetHighlighting();
        group.eachLayer(function (layer) {
            if (layer.feature.properties.osm_id === featureId) {
                layer.setStyle({fillColor: 'red', color: 'red'})
            }
        });
    }

    $('a[data-toggle="tab"]').on('shown.bs.tab', function (e) {
        const id = parseInt(e.target.id.substr(8));
        highlightFeature(id);
    });

    mymap.on('contextmenu', function (e) {
        const coord = e.latlng;
        const lat = coord.lat;
        const lon = coord.lng;

        const urlParams = new URLSearchParams(window.location.search);
        urlParams.set("lat", lat);
        urlParams.set("lon", lon);
        urlParams.delete("ids");

        window.location = "/api/v1/reverse?" + urlParams.toString();
    });
</script>
</body>
</html>