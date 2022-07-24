    document.querySelector('.form-holder').style.marginLeft =  "0";
    document.querySelector('.form-items').style.maxWidth =  "500px";
    document.querySelector('.website-logo').style.top =  "50px";
    document.querySelector('.website-logo').style.left =  "50px";
    document.querySelector('.website-logo').style.right =  "initial";
    document.querySelector('.website-logo').style.bottom =  "initial";

checkJournal(formatCurrentMysqlDateTime(), usr);

function prepareJournal() {
    let jrnl = {};
    jrnl.user = usr;

    classie.remove(document.querySelector('.frm-journal'), 'hide');
    if (document.querySelector('.txtarea')) {
        document.querySelector('.txtarea').addEventListener("keyup", function(event) {
            const key = event.key;
            jrnl.message = this.value.trim();
        });
    }
    if (document.querySelector('.btn-create-journal')) {
        document.querySelector('.btn-create-journal').addEventListener("click", function(ev) {
            ev.stopPropagation();
            ev.preventDefault();
            postJournal(jrnl);
        });
    }
}

function formatCurrentMysqlDateTime() { //2022-04-07 07:37:32
    return new Date().toISOString().slice(0, 10) + " " + new Date().toLocaleTimeString('en-GB');
}


function postJournal(j) {
    j.created = formatCurrentMysqlDateTime();
    console.log(j);
    weHavePostJournalSuccess = false;
    $.ajax({
        type: "POST",
        url: "/journal",
        contentType: "application/json;charset=UTF-8",
        async: true,
        data: JSON.stringify({
            "jrn": j
        }),
        success: function(data, status, xhr) {
            weHavePostJournalSuccess = true;
            var params = JSON.parse(xhr.responseText);
            //console.log(params);
            location.reload();
        },
        error: function(xhr, status, error) {
            //console.log("Error!" + xhr.status);
            console.log(xhr.statusText);
        },
        complete: function() {
            if (!weHavePostJournalSuccess) {
                //console.log('Sent Data');
            }
        },
        dataType: "json"
    });
}

//checking whether there has been a journal entry today
function checkJournal(jd, u) {
    weHavecheckJournalSuccess = false;
    $.ajax({
        type: "POST",
        url: "/journal/check",
        contentType: "application/json;charset=UTF-8",
        async: true,
        data: JSON.stringify({
            "jrnd": {
            "user": u,
            "date": jd
            }
        }),
        success: function(data, status, xhr) {
            weHavecheckJournalSuccess = true;
            var params = JSON.parse(xhr.responseText);
            console.log(params);
            if (params.length == 0) { //no journals
                prepareJournal();
                //update journal for this user, no journal
            } else { //yes journal
                classie.add(document.querySelector('.frm-journal'), 'hide');
                //location.href = '/journals';
            }
        },
        error: function(xhr, status, error) {
            //console.log("Error!" + xhr.status);
            console.log(xhr.statusText);
        },
        complete: function() {
            if (!weHavecheckJournalSuccess) {
                //console.log('Sent Data');
            }
        },
        dataType: "json"
    });
}