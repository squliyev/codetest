
let servicesRequest = new Request('/get-services');
fetch(servicesRequest)
.then(function(response) { return response.json(); })
.then(function(serviceList) {
    fillServiceList(serviceList);
});

// fill service list element in html
var fillServiceList = function (serviceList) {
    const listContainer = document.querySelector('#service-list');
    if(serviceList.length) {
        listContainer.append(createHeader());
    }
    serviceList.forEach(service => {
        var li = document.createElement("li");
        li.appendChild(createElement("div", service.name, [{ name: "class", value: "name" }]));
        li.appendChild(createElement("div", service.url, [{ name: "class", value: "url" }]));
        li.appendChild(createElement("div", service.date, [{ name: "class", value: "date" }]));
        li.appendChild(createElement("div", service.status, [{ name: "class", value: "status" }]));
        li.appendChild(createButton("delete", service, deleteFunction));
        li.appendChild(createButton("update", service, updateFunction));
        listContainer.appendChild(li);
    });
}

// Helper method for creating header for list
var createHeader = function() {
    let li = document.createElement("li");
    let headerList = [
        ["Service Name", "name"],
        ["Service Url", "url"],
        ["Entry Date", "date"],
        ["Status", "status"],
        ["Delete", "delete"],
        ["Update", "update"]];
    li.setAttribute("class","li-header");
    headerList.forEach(x => {
        li.appendChild(createElement("div", x[0], [{ name: "class", value: x[1] }]));
    })
    return li;
}

// Helper method or creating new html elements
var createElement = function(name, text, attributes, eventFunction) {
    let element = document.createElement(name);
    attributes.forEach(x=> {
        element.setAttribute(x.name, x.value);
    });
    element.append(document.createTextNode(text));
    if(eventFunction) {
        eventFunction(element);
    }
    return element;
}

// Helper method for creating button
var createButton = function (buttonName, service, onClickfunction) {
    let wrapper = createElement("div", "",[{ name: "class", value: buttonName }]);
    wrapper.appendChild(createElement("button", buttonName,
        [
            { name: "class", value: buttonName + "-btn btn" },
            { name: "url", value: service.url }
        ], onClickfunction));
    return wrapper;
}

// delete functionality (onClickfunction)
var deleteFunction = function(button) {
    let myFunction = function () {
        let serviceName = this.getAttribute("url");
        if(serviceName.length && confirm("Are you sure?")) {
            fetch('/delete-service', {
                method: 'post',
                headers: {
                    'Accept': 'application/json, text/plain, */*',
                    'Content-Type': 'application/json'
                }, body: JSON.stringify({url:serviceName})
            }).then(res=> location.reload());
        }
    }
    button.addEventListener('click', myFunction, false);
};

// update functionality (onClickfunction)
var updateFunction = function (button) {
    let dialog = document.getElementById("update-dialog");
    let myFunction = function () {
        let serviceName = this.parentElement.parentElement.querySelector(".name").innerHTML;
        let serviceUrl = this.getAttribute("url");
        document.getElementById("service-name-update").value = serviceName;
        var dialogUrl = document.getElementById("service-url-update");
        dialogUrl.value = serviceUrl;
        dialogUrl.setAttribute("primary-url", serviceUrl);
        dialog.classList.add("display-block");
    }
    button.addEventListener('click', myFunction, false);
}

// save functionality
const saveButton = document.querySelector('#post-service');
saveButton.onclick = evt => {
    let serviceName = document.querySelector('#service-name').value;
    let serviceUrl = document.querySelector('#url-name').value;
    if(serviceUrl.length) {
        fetch('/insert-service', {
            method: 'post',
            headers: {
                'Accept': 'application/json, text/plain, */*',
                'Content-Type': 'application/json'
            }, body: JSON.stringify({name:serviceName,  url:serviceUrl})
        }).then(res=> {
            if(res.status != 400) {
                location.reload();
            }
            else {
                alert("Incorrect Url");
            }
        });
    }
}

// continuous status checker
var isStatusCheckerRunning = false;
var statusChecker = setInterval(function () {
    if (isStatusCheckerRunning == false) {
        isStatusCheckerRunning = true;
        let servicesRequest = new Request('/check-services');
        let urls = Array.from(document.getElementsByClassName('url'));
        fetch(servicesRequest)
            .then(function(response) { return response.json(); })
            .then(function(serviceList) {
                if(!serviceList.length) return;
                serviceList.forEach(service => {
                    if(service.status == "unknown")
                        return;
                    let url = urls.find(el => el.textContent === service.url);
                    let statusDiv = url.parentElement.querySelector(".status");
                    let clr =statusDiv.style.color;
                    statusDiv.style.color = "#ff5A60";
                    setTimeout(function() {
                        statusDiv.innerHTML =  service.status;
                    }, 500);
                    setTimeout(function() {
                        statusDiv.style.color = clr;
                    }, 500);

                });
                isStatusCheckerRunning = false;
            });
    }
}, 3000);

// dialog close functionality
var dialogCloseFunction = function () {
    let dialog = document.getElementById("update-dialog");
    dialog.classList.remove("display-block");
}
var dialogClose = document.querySelector(".dialog-close")
dialogClose.addEventListener('click', dialogCloseFunction, false);


// dialog update service functionality
var updateService = function () {
    let serviceName = document.getElementById("service-name-update").value;
    let dialogUrl = document.getElementById("service-url-update");
    let serviceUrl = dialogUrl.value;
    let primaryUrl = dialogUrl.getAttribute("primary-url")
    fetch('/update-service', {
        method: 'post',
        headers: {
            'Accept': 'application/json, text/plain, */*',
            'Content-Type': 'application/json'
        }, body: JSON.stringify({name:serviceName,  url:serviceUrl, primaryUrl:primaryUrl})
    }).then(res=> {
        if(res.status != 400) {
            location.reload();
        }
        else {
            alert("Incorrect Url");
        }
    });
}
var updateServiceButton = document.querySelector(".update-button")
updateServiceButton.addEventListener('click', updateService, false);