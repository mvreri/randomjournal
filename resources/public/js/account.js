function validateEmail(email) {
    var re = /^(([^<>()[\]\\.,;:\s@\"]+(\.[^<>()[\]\\.,;:\s@\"]+)*)|(\".+\"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
    return re.test(email);
}

function validateAlphanumeric(value) {
    var re = /^[A-Za-z0-9 ]+$/i;
    return re.test(value);
}

if (document.querySelector('.btn-login')) {
    document.querySelector('.btn-login').addEventListener("click", function(ev) {
        ev.stopPropagation();
        ev.preventDefault();
        console.log('loggin in');
        //validate hre
        if (!validateEmail(document.querySelector('.uname').value)) {
            console.log('fix email');
            return false;
        }
        if (document.querySelector('.password').value.length < 6) {
            console.log('fix passw');
            return false;
        }
        classie.add(document.querySelector('.btn-login'), 'hide');
        var lg = {};
        lg.email = document.querySelector('.uname').value,
            lg.password = document.querySelector('.password').value;
        login(lg);
    });
}
if (document.querySelector('.btn-register')) {
    document.querySelector('.btn-register').addEventListener("click", function(ev) {
        ev.stopPropagation();
        ev.preventDefault();
        console.log('registerin');
        //validate
        if (!validateAlphanumeric(document.querySelector('.fname').value)) {
            console.log('fix name');
            return false;
        }
        if (!validateEmail(document.querySelector('.email').value)) {
            console.log('fix email');
            return false;
        }
        if (document.querySelector('.password').value.length < 6) {
            console.log('fix password');
            return false;
        }
        classie.add(document.querySelector('.btn-register'), 'hide');
        var rc = {};
        rc.name = document.querySelector('.fname').value,
            rc.email = document.querySelector('.email').value,
            rc.password = document.querySelector('.password').value;
        register(rc);
    });
}

function register(reg) {
    weHaveregSuccess = false;
    $.ajax({
        type: "POST",
        url: "/register",
        contentType: "application/json;charset=UTF-8",
        async: true,
        data: JSON.stringify({
            "registercreds": reg
        }),
        success: function(data, status, xhr) {
            weHaveregSuccess = true;
            var params = JSON.parse(xhr.responseText);
            if (params.hasOwnProperty('data')) {
                location.href = '/login';
            } else {
                classie.remove(document.querySelector('.btn-register'), 'hide');
                console.log(params.errors.detail);
            }
        },
        error: function(xhr, status, error) {
            //console.log("Error!" + xhr.status);
            console.log(xhr.statusText);
            classie.remove(document.querySelector('.btn-register'), 'hide');
        },
        complete: function() {
            if (!weHaveregSuccess) {
                //console.log('Sent Data');
            }
        },
        dataType: "json"
    });
}


function login(lgn) {
    weHaveloginSuccess = false;
    $.ajax({
        type: "POST",
        url: "/login",
        contentType: "application/json;charset=UTF-8",
        async: true,
        data: JSON.stringify({
            "logincreds": lgn
        }),
        success: function(data, status, xhr) {
            weHaveloginSuccess = true;
            var params = JSON.parse(xhr.responseText);
            if (params.hasOwnProperty('data')) {
                location.href = '/journal';
            } else {
                classie.remove(document.querySelector('.btn-login'), 'hide');
                console.log(params.errors.detail);
            }
        },
        error: function(xhr, status, error) {
            //console.log("Error!" + xhr.status);
            console.log(xhr.statusText);
            classie.remove(document.querySelector('.btn-login'), 'hide');
        },
        complete: function() {
            if (!weHaveloginSuccess) {
                //console.log('Sent Data');
            }
        },
        dataType: "json"
    });
}