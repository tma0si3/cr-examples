/*
 *                                            Bosch SI Example Code License
 *                                              Version 1.0, January 2016
 *
 * Copyright 2016 Bosch Software Innovations GmbH ("Bosch SI"). All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the 
 * following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials provided with the distribution.
 * 
 * BOSCH SI PROVIDES THE PROGRAM "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE ENTIRE RISK AS TO
 * THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH YOU. SHOULD THE PROGRAM PROVE DEFECTIVE, YOU ASSUME THE COST OF 
 * ALL NECESSARY SERVICING, REPAIR OR CORRECTION. THIS SHALL NOT APPLY TO MATERIAL DEFECTS AND DEFECTS OF TITLE WHICH 
 * BOSCH SI HAS FRAUDULENTLY CONCEALED. APART FROM THE CASES STIPULATED ABOVE, BOSCH SI SHALL BE LIABLE WITHOUT
 * LIMITATION FOR INTENT OR GROSS NEGLIGENCE, FOR INJURIES TO LIFE, BODY OR HEALTH AND ACCORDING TO THE PROVISIONS OF
 * THE GERMAN PRODUCT LIABILITY ACT (PRODUKTHAFTUNGSGESETZ). THE SCOPE OF A GUARANTEE GRANTED BY BOSCH SI SHALL REMAIN
 * UNAFFECTED BY LIMITATIONS OF LIABILITY. IN ALL OTHER CASES, LIABILITY OF BOSCH SI IS EXCLUDED. THESE LIMITATIONS OF 
 * LIABILITY ALSO APPLY IN REGARD TO THE FAULT OF VICARIOUS AGENTS OF BOSCH SI AND THE PERSONAL LIABILITY OF BOSCH SI'S
 * EMPLOYEES, REPRESENTATIVES AND ORGANS.
 */

"use strict";

$(document).ready(function () {

    // --- Variable for ThingID
    var thingId;

    // --- Get ThingID from input
    var getThingId = function () {

        var elem = document.getElementById('input');
        var input = document.getElementById('submit');
        thingId = document.getElementById('input').value;
        if (thingId == null || thingId == "") {
            alert("Please insert valid ThingID!");
            return;
        }
        $('#thingID').html(thingId.toString());
        elem.style.display = 'none';
        input.style.display = 'none';
        refreshDetails();
    };

    var refreshDetails = function () {

        $.getJSON("cr/1/things/" + thingId).done(function (thing, textStatus) {

            // --- clear table content and remember thingId
            $("#detailsThingId").text(thingId);
            var tablebody = $("#detailsTableBody");
            tablebody.empty();

            if ("attributes" in thing) {
                // --- for each attribute put row in details table
                var attrNames = Object.getOwnPropertyNames(thing.attributes);
                var first = true;
                attrNames.forEach(function (attribute) {
                    var value = thing.attributes[attribute];
                    var row = $("<tr>");
                    if (first) {
                        row.append($("<td rowspan=" + attrNames.length + ">").text("Attribute"));
                        first = false;
                    }
                    row.append($("<td>").text(attribute));
                    row.append($("<td>").text(typeof value == "object" ? JSON.stringify(value, null, 3) : value));
                    tablebody.append(row);
                });
            }

            $("#details").show();

        }).fail(function () {
            $("#details").hide();
            $("#failure").show();
        });

        if ($("#autoRefresh").is(":checked")) {
            window.setTimeout(refreshDetails, 1000);
        }
    };

    $("#submit").click(getThingId);
    $("#failure").hide();

    $("#refreshAttr").click(refreshDetails);
    $("#autoRefresh").on("change", function () {
        if ($("#autoRefresh").is(":checked")) {
            window.setTimeout(refreshDetails, 1000);
        }
    });
});