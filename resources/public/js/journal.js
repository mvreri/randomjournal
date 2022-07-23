checkJournal(formatCurrentMysqlDateTime());

function prepareJournal() {
    let jrnl = {};
    jrnl.user = 'thisuser';
classie.remove(document.querySelector('.frm-journal'), 'hide');
    if (document.querySelector('.txtarea')) {
        document.querySelector('.txtarea').addEventListener("keyup", function(event) {
            const key = event.key;
            console.log(this.value);
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
            console.log(params);
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
function checkJournal(jd) {
    weHavecheckJournalSuccess = false;
    $.ajax({
        type: "POST",
        url: "/journal/check",
        contentType: "application/json;charset=UTF-8",
        async: true,
        data: JSON.stringify({
            "jrnd": jd
        }),
        success: function(data, status, xhr) {
            weHavecheckJournalSuccess = true;
            var params = JSON.parse(xhr.responseText);
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