// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

/* 
 * Fade in items on scroll when their upper bound crosses the bottom of 
 * of the screen.
 */
$(window).on("load", function() {
    $(window).scroll(function() {
        var window_bottom = $(this).scrollTop() + $(this).innerHeight();

        $(".fade").each(function() {
            var obj_bottom = $(this).offset().top + ($(this).outerHeight() / 4.0);
            if (obj_bottom < window_bottom) {
                if ($(this).css("opacity")==0) {
                    $(this).fadeTo(500, 1);
                }
            } else {
                if ($(this).css("opacity") == 1) {
                    $(this).fadeTo(500, 0);
                }
            }
        });
    }).scroll();
});

/*
 * Retrieve comments from database. Set limit on the number of comments
 * retrieved through query parameter.
 */
function addCommentsToDOM(limit) {
    fetch("/data?limit=".concat(limit))
    .then(response => response.json())
    .then((comments) => {
        const container = document.getElementById('msg-container');
        container.innerHTML = '';
        comments.forEach((line) => {
            /* Create comment and add text. */
            const node = document.createElement("p");
            const pnode = document.createTextNode(line);
            node.appendChild(pnode);
            container.appendChild(node);
        })
    })
}

/*
 * Load Google Maps.
 */
function createMap(map_name, latitude, longitude) {
    const map = new google.maps.Map(
        document.getElementById(map_name),
        {center: {lat: latitude, lng: longitude}, zoom: 12}
    );
}

/*
 * Append set of footprints to the DOM, with direction pointing top to bottom,
 * fading in on scroll, to emulate the experience of walking from a location
 * at the top of the screen to the bottom. Takes the ID of the destination 
 * location as an argument.
 */
function createFootprints(locationID) {
    const num_footprints = 6;
    const location = document.getElementById(locationID);
    for (let i = 0; i < num_footprints; i++) {
        const footprint = document.createElement("img");
        footprint.src = "images/footprint.png";
        const direction = (i % 2 == 0) ? "right" : "left";
        footprint.classList.add("footprint-" + direction);

        /* Vary width between steps for a more natural appearance. */
        const rand = Math.floor(Math.random * (3) + 2);
        if (direction.localeCompare("right") == 0) {
            footprint.style.margin = "1rem 1rem 1rem " + rand + "rem";
        } else {
            footprint.style.margin = "1rem " + rand + "rem 1rem 1rem";
        }
        
        footprint.classList.add("fade");
        location.appendChild(footprint);
    }
}
