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
        //validate hre
        if (!validateEmail(document.querySelector('.uname').value)) {
             Swal.fire({
               title: 'Email Address',
               html: 'There\'s an issue with the email address specified',
               icon: 'warning',
               confirmButtonText: 'Okay'
             });
            return false;
        }
        if (document.querySelector('.password').value.length < 6) {
             Swal.fire({
               title: 'Password',
               html: 'There\'s an issue with the length of the password specified',
               icon: 'warning',
               confirmButtonText: 'Okay'
             });
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
        //validate
        if (!validateAlphanumeric(document.querySelector('.fname').value)) {
             Swal.fire({
               title: 'Name',
               html: 'Please check the name specified',
               icon: 'warning',
               confirmButtonText: 'Okay'
             });
            return false;
        }
        if (!validateEmail(document.querySelector('.email').value)) {
             Swal.fire({
               title: 'Email Address',
               html: 'There\'s an issue with the email address specified',
               icon: 'warning',
               confirmButtonText: 'Okay'
             });
             document.querySelector('.email').focus();
            return false;
        }
        if (document.querySelector('.password').value.length < 6) {
             Swal.fire({
               title: 'Password',
               html: 'There\'s an issue with the length of the password specified',
               icon: 'warning',
               confirmButtonText: 'Okay'
             });
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
             Swal.fire({
               title: 'Register',
               html: params.errors.detail,
               icon: 'warning',
               confirmButtonText: 'Okay'
             });
            }
        },
        error: function(xhr, status, error) {
            //console.log("Error!" + xhr.status);
            classie.remove(document.querySelector('.btn-register'), 'hide');
             Swal.fire({
               title: 'Register',
               html: xhr.statusText,
               icon: 'error',
               confirmButtonText: 'Okay'
             });
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
             Swal.fire({
               title: 'Login',
               html: params.errors.detail,
               icon: 'warning',
               confirmButtonText: 'Okay'
             });
            }
        },
        error: function(xhr, status, error) {
            //console.log("Error!" + xhr.status);
            classie.remove(document.querySelector('.btn-login'), 'hide');
             Swal.fire({
               title: 'Login',
               html: xhr.statusText,
               icon: 'error',
               confirmButtonText: 'Okay'
             });
        },
        complete: function() {
            if (!weHaveloginSuccess) {
                //console.log('Sent Data');
            }
        },
        dataType: "json"
    });
}