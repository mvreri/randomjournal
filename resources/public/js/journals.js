getJournals(usr);

function getJournals(usr) {
    weHavegetJournalSuccess = false;
    $.ajax({
        type: "POST",
        url: "/journals",
        contentType: "application/json;charset=UTF-8",
        async: true,
        data: JSON.stringify({
            "jrnusr": usr
        }),
        success: function(data, status, xhr) {
            weHavegetJournalSuccess = true;
            var params = JSON.parse(xhr.responseText);
            if (params.hasOwnProperty('data')) {
            while (document.querySelector('.tr-journals').firstChild) {
                document.querySelector('.tr-journals').removeChild(document.querySelector('.tr-journals').firstChild);
            }
                if (params.data.detail.length > 0) {
                    for (let i = 0; i < params.data.detail.length; i++) {
                        var jns = JSON.parse(params.data.detail[i].details);

                        document.querySelector('.tr-journals')
                            .appendChild(jQuery(`<tr>
                                    <td class="user-name">` + jns.created + `</td>
                                    <td class="user-email">` + jns.message + `</td>
                                  </tr>`)[0]);
                    }
                }
            } else {
                console.log(params.errors.detail);
            }


        },
        error: function(xhr, status, error) {
            console.log(xhr.statusText);
        },
        complete: function() {
            if (!weHavegetJournalSuccess) {
                //console.log('Sent Data');
            }
        },
        dataType: "json"
    });
}

//getRandomJournals(usr);

function getRandomJournals(usr) {
    weHavegetRandomJournalsSuccess = false;
    $.ajax({
        type: "POST",
        url: "/journals/random",
        contentType: "application/json;charset=UTF-8",
        async: true,
        data: JSON.stringify({
            "jrnusr": usr
        }),
        success: function(data, status, xhr) {
            weHavegetRandomJournalsSuccess = true;
            var params = JSON.parse(xhr.responseText);

            while (document.querySelector('.tr-journals').firstChild) {
                document.querySelector('.tr-journals').removeChild(document.querySelector('.tr-journals').firstChild);
            }
            if (params.length > 0) {
                for (let i = 0; i < params.length; i++) {
                    var jns = JSON.parse(params[i].details);
                    document.querySelector('.tr-journals')
                        .appendChild(jQuery(`<tr>
                                    <td class="user-name">` + jns.created + `</td>
                                    <td class="user-email">` + jns.message + `</td>
                                  </tr>`)[0]);
                }
            }

        },
        error: function(xhr, status, error) {
            //console.log("Error!" + xhr.status);
            console.log(xhr.statusText);
        },
        complete: function() {
            if (!weHavegetRandomJournalsSuccess) {
                //console.log('Sent Data');
            }
        },
        dataType: "json"
    });
}