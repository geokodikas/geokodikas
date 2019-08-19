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
            <a class="btn btn-primary" href="${as_json_link}" role="button">JSON</a>
            <br><br>
            <details>
                <pre><code>${geojson}</code></pre>
            </details>
            <br>
            <ul class="list-group">
                <li class="list-group-item list-group-item-primary">${osm_id?c}</li>
                <li class="list-group-item">Type is "${osm_type}"</li>
                <li class="list-group-item">Layer is "${layer}"</li>
                <li class="list-group-item list-group-item-${has_one_way_restriction?string('success', 'danger')}">${has_one_way_restriction?string('Has one way restirction', 'No one way restiction')}</li>
                <li class="list-group-item list-group-item-${has_reversed_one_way?string('success', 'danger')}">${has_reversed_one_way?string('One way is reversed', 'One way is not reversed')}</li>
            </ul>
            <br>
            ${parent_html}
            <br>
            ${tags_html}
        </div>
    </div>
</div>


<script>
    let geojsonFeature = ${geojson};

    let mymap = L.map('mapid'); //.setView([51.505, -0.09], 13);

    let group = L.featureGroup([L.geoJSON(geojsonFeature)])
        .addTo(mymap);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: 'Map data © <a href="https://openstreetmap.org">OpenStreetMap</a> contributors',
        minZoom: 8,
        maxZoom: 18
    }).addTo(mymap);

    mymap.fitBounds(group.getBounds());
</script>
</body>
</html>