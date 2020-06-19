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
 * If user is not logged in, create a login button that will allow the user to
 * login and add it to the DOM. Otherwise, create the comment input form and 
 * the button to restrict number of comments and add them to the DOM.
 */
function getLogin() {
    fetch("/login")
    .then(response => response.json())
    .then((result) => {
        const button_area = document.getElementById("button-area");
        const msg_form = document.getElementById("msg-form");
        button_area.innerHTML = '';
        let button = document.createElement('button');
        if (result.loggedIn == 1) {
            /* Allow logged in users to submit comments. */
            let form = document.createElement('form');
            form.id = "comment-form";
            form.action = "/data";
            form.method = "POST";

            let textarea = document.createElement('textarea');
            textarea.id = "comment-form-box";
            textarea.name = "comment";
            textarea.placeholder = "LEAVE A COMMENT!";
            form.appendChild(textarea);

            let line_break = document.createElement('br');
            form.appendChild(line_break);

            let form_input = document.createElement('input');
            form_input.id = "comment-form-input";
            form_input.type = "submit";
            form.appendChild(form_input);
            form.style.marginTop = "10rem";
            msg_form.appendChild(form);
            msg_form.style.width = "75%";

            /* Allow logged in users to change the number of comments. */
            let input = document.createElement('input');
            input.addEventListener('change', () => { getComments(input.value); });
            input.type = "number";
            input.id = "quantitiy";
            input.name = "limit";
            input.min = "0";
            input.placeholder = "MAX COMMENTS";
            input.style.padding = "0.5rem";
            input.style.margin = "1rem";
            button_area.append(input);

            button.setAttribute('id', 'logout');
            button.innerHTML = "LOGOUT";
            button.addEventListener('click', () => {
                window.location.href = result.URL;
            });
            button.style.padding = "0.5rem";
            button.style.margin = "1rem";
            button_area.appendChild(button);
        } else {
            /* Create a login button that redirects to login page when clicked. */
            button.setAttribute('id', 'login');
            button.innerHTML = "LOGIN";
            button.addEventListener('click', () => {
                window.location.href = result.URL;
            });
            button_area.style.padding = "0.5rem";
            button.style.margin = "1rem";
            button_area.appendChild(button);
        }
    });
}

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
    if (limit == '')
        return;
    fetch("/data?limit=".concat(limit))
    .then(response => response.json())
    .then((comments) => {
        const container = document.getElementById('msg-container');
        container.innerHTML = '';
        comments.forEach((entry) => {
            const comment = document.createElement('div');
            comment.style.display = "flex";

            /* Create comment and add text and delete icon. */
            let text = entry.comment;
            const node = document.createElement('p');
            const pnode = document.createTextNode(text);
            node.appendChild(pnode);
            comment.appendChild(node);

            const del = document.createElement('input');
            del.type = "image";
            del.src = "images/x_icon.png";
            del.addEventListener('click', () => {
                deleteComment(entry);
                comment.remove();
            });
            del.classList.add('delete-button');
            comment.appendChild(del);
            container.append(comment);
        })
    })
}

/*
 * Load Google Maps.
 */
function createMap(map_name, latitude, longitude) {
    let myLatLng = {lat: latitude, lng: longitude};

    const map = new google.maps.Map(document.getElementById(map_name), {
        center: myLatLng,
        zoom: 12
    });

    let marker = new google.maps.Marker({
        position: myLatLng,
        map: map,
    });
}

/*
 * Append a set of footprints to the DOM, with direction pointing top to bottom,
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
        const rand = Math.floor(Math.random * (4) + 3);
        if (direction.localeCompare("right") == 0) {
            footprint.style.margin = "1rem 1rem 1rem " + rand + "rem";
        } else {
            footprint.style.margin = "1rem " + rand + "rem 1rem 1rem";
        }
        footprint.classList.add("fade");
        location.appendChild(footprint);
    }
}

/* 
 * fsdf   sdsdfsent using ID as identifier. Does not redirect page after
 * deletion.
 */
function deleteComment(comment) {
    const params = new URLSearchParams();
    params.append('id', comment.id);
    fetch('/delete-data', {
        method: 'POST',
        body: params
    });
}
