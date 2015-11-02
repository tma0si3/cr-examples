/*
 Copyright (c) 2015, Bosch Software Innovations GmbH, Germany
 All rights reserved.

 Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer
 in the documentation and/or other materials provided with the distribution.

 3. Neither the name of the Bosch Software Innovations GmbH, Germany nor the names of its contributors
 may be used to endorse or promote products derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
 OF SUCH DAMAGE.
 */

"use strict";

$(document).ready(function () {

    // --- Handler for refreshing details
    var refreshDetails = function () {
        var thingId = $("#details").attr("thingId");
        $.getJSON("cr/1/things/" + thingId).done(function (thing, textStatus) {

            $("#detailsThingId").text(thingId);

            var tablebody = $("#detailsTableBody");
            tablebody.empty();

            if ("attributes" in thing) {
                // --- for each attribute put row in details table
                var attrNames = Object.getOwnPropertyNames(thing.attributes);
                if (attrNames.indexOf("_features") != -1)
                {
                    attrNames.splice(attrNames.indexOf("_features"), 1);
                }
                var first = true;
                attrNames.forEach(function (attribute) {
                    var value = thing.attributes[attribute];
                    var row = $("<tr>");
                    if (first) {
                        row.append($("<td rowspan=" + attrNames.length + ">").text("Attribute"));
                        first = false;
                    }
                    row.append($("<td>").text(attribute));
                    row.append($("<td>").text(typeof value == "object" ? JSON.stringify(value) : value));
                    tablebody.append(row);
                });

                if ("_features" in thing.attributes) {
                    // --- for each feature property put row in details table
                    Object.getOwnPropertyNames(thing.attributes._features).forEach(function (featureId) {
                        var feature = thing.attributes._features[featureId];
                        var propNames = Object.getOwnPropertyNames(feature.properties);
                        var first = true;
                        propNames.forEach(function (prop) {
                            var value = feature.properties[prop];
                            var row = $("<tr>");
                            if (first) {
                                row.append($("<td rowspan=" + propNames.length + ">").text("Feature \"" + featureId + "\""));
                                first = false;
                            }
                            row.append($("<td>").text(prop));
                            row.append($("<td>").text(typeof value == "object" ? JSON.stringify(value) : value));
                            tablebody.append(row);
                        });
                    });
                }
            }

            $("#details").show();
        }).fail(function () {
            $("#details").hide();
        });
    };

    // --- Handler for refreshing list and map of things
    var refreshTable = function () {

        $.getJSON("cr/1/search/things?fields=thingId,attributes/name,attributes/_features/geolocation").done(function (data, textStatus) {

            // --- clear table content and clear map
            $("#tableBody").empty();
            if (markers != null) {
                map.removeLayer(markers);
            }

            // new marker layer on map
            markers = new L.FeatureGroup();
            map.addLayer(markers);

            // iterate of retrieved things
            var count = data.items.length;
            for (var i = 0; i < count; i++) {
                var t = data.items[i];

                // --- add heading data to table
                var row = $("<tr>");
                row.attr("thingId", t.thingId);
                row.append($("<td>").text(t.thingId));

                if ("attributes" in t && "name" in t.attributes) {
                    row.append($("<td>").text(t.attributes.name));
                }
                else {
                    row.append($("<td>").text("-"));
                }
                $("#tableBody").append(row);

                // --- when thing has a "geolocation" feature with "geoposition" properties
                //if ("features" in t && "geolocation" in t.features && "geoposition" in t.features.geolocation.properties) {
                if ("attributes" in t && "_features" in t.attributes && "geolocation" in t.attributes._features
                    && "geoposition" in t.attributes._features.geolocation.properties) {

                    // --- add marker for thing on map
                    //var latlng = [t.features.geolocation.properties.geoposition.latitude,
                    //    t.features.geolocation.properties.geoposition.longitude];
                    var latlng = [t.attributes._features.geolocation.properties.geoposition.latitude,
                        t.attributes._features.geolocation.properties.geoposition.longitude];
                    var marker = L.marker(latlng);
                    marker._thingId = t.thingId;
                    marker.bindPopup(t.thingId);
                    marker.on("click", function (e) {
                        $("#details").attr("thingId", e.target._thingId);
                        refreshDetails();
                    });
                    marker.addTo(markers);
                }
            }

            if ($("#details").attr("thingId")) {
                refreshDetails();
            }

        });
    };


    // --- create map
    var map = L.map("map");
    var osm = new L.TileLayer("http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
        minZoom: 5, maxZoom: 20,
        attribution: "Map data &copy; <a href=\"http://openstreetmap.org\">OpenStreetMap</a> contributors"
    });
    map.setView(new L.LatLng(47.682085, 9.386510), 13);
    map.addLayer(osm);
    var markers = null;

    $("#refreshTable").click(refreshTable);

    $("#tableBody").on("click", "tr", function () {
        var row = $(this);
        var thingId = row.attr("thingId");

        // --- inactivate old row and activate clicked row
        $("#tableBody").find("tr.active").removeClass("active");
        row.addClass("active");

        // --- refresh thing details
        $("#details").attr("thingId", thingId);
        refreshDetails();
    });

    refreshTable();

});