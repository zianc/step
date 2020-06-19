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
 * Get the login status of the current user. If logged in, display the comment
 * submission input box and the button to select number of displayed comments.
 * Display a logout button. If not logged in, hide the comment submission and 
 * selection of number of displayed comments. Display a login button.
 */
function getLoginStatus() {
    fetch('/login')
    .then(response => response.json())
    .then((result) => {
        const button_area = document.getElementById('button-area');
        const msg_form = document.getElementById('msg-form');
        const comment_form = document.getElementById('comment-form');
        const quantity_input = document.getElementById('quantity');
        const login_btn = document.getElementById('login-btn');
        const logout_btn = document.getElementById('logout-btn');
 
        if (result.loggedIn == 1) {
            /* Allow logged in users to submit comments and change number of comments. */
            comment_form.style.display = 'flex';
            quantity_input.style.display = 'block';
 
            /* Display logout button. */
            logout_btn.addEventListener('click', () => {
                window.location.href = result.URL;
            });
            logout_btn.style.display = 'block';
            login_btn.style.display = 'none';
            
        } else {
            /* If not logged in, hide comment submission form and comment restriction option. */
            comment_form.style.display = 'none';
            quantity_input.style.display = 'none';
 
            /* Display login button. */
            login_btn.addEventListener('click', () => {
                window.location.href = result.URL;
            });
            login_btn.style.display = 'block';
            logout_btn.style.display = 'none';
        }
    });
}
 
/* 
 * Fade in items on scroll when their upper bound crosses the bottom of 
 * of the screen.
 */
$(window).on('load', function() {
    $(window).scroll(function() {
        var window_bottom = $(this).scrollTop() + $(this).innerHeight();
 
        $('.fade').each(function() {
            var obj_bottom = $(this).offset().top + ($(this).outerHeight() / 4.0);
            if (obj_bottom < window_bottom) {
                if ($(this).css('opacity')==0) {
                    $(this).fadeTo(500, 1);
                }
            } else {
                if ($(this).css('opacity') == 1) {
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
    fetch('/data?limit='.concat(limit))
    .then(response => response.json())
    .then((comments) => {
        const container = document.getElementById('msg-container');
        container.innerHTML = '';
        comments.forEach((entry) => {
            const comment = document.createElement('div');
            comment.style.display = 'flex';
 
            /* Create comment and add text and delete icon. */
            let text = entry.comment;
            const node = document.createElement('p');
            const pnode = document.createTextNode(text);
            node.appendChild(pnode);
            comment.appendChild(node);

            const del = document.createElement('input');
            del.type = 'image';
            del.src = 'images/x_icon.png';
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
 * Creates set of footprints from one div to the next. Each footprint is given
 * a random horizontal positioning between 3rem to 5rem off center.
 */
function createFootprints(elem_name) {
    const num_footprints = 6;
    const elem = document.getElementById(elem_name);
    for (let i = 0; i < num_footprints; i++) {
        const footprint = document.createElement('img');
        footprint.src = 'images/footprint.png';
        const direction = (i % 2 == 0) ? 'right' : 'left';
        footprint.classList.add('footprint-' + direction);
        const rand = Math.floor(Math.random * (4) + 3);
        if (direction.localeCompare('right') == 0) {
            footprint.style.margin = '1rem 1rem 1rem ' + rand + 'rem';
        } else {
            footprint.style.margin = '1rem ' + rand + 'rem 1rem 1rem';
        }
        footprint.classList.add('fade');
        elem.appendChild(footprint);
    }
}
 
/* 
 * Delete single comment using ID as identifier. Does not redirect page after
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
