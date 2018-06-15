"use strict";

// --------
// WARNING:
// --------

// THIS CODE IS ONLY MADE AVAILABLE FOR DEMONSTRATION PURPOSES AND IS NOT SECURE!
// DO NOT USE IN PRODUCTION!

// FOR SECURITY REASONS, USING A JAVASCRIPT WEB APP HOSTED VIA THE CORDA NODE IS
// NOT THE RECOMMENDED WAY TO INTERFACE WITH CORDA NODES! HOWEVER, FOR THIS
// PRE-ALPHA RELEASE IT'S A USEFUL WAY TO EXPERIMENT WITH THE PLATFORM AS IT ALLOWS
// YOU TO QUICKLY BUILD A UI FOR DEMONSTRATION PURPOSES.

// GOING FORWARD WE RECOMMEND IMPLEMENTING A STANDALONE WEB SERVER THAT AUTHORISES
// VIA THE NODE'S RPC INTERFACE. IN THE COMING WEEKS WE'LL WRITE A TUTORIAL ON
// HOW BEST TO DO THIS.

const app = angular.module('demoAppModule', ['ui.bootstrap']);

// Fix for unhandled rejections bug.
app.config(['$qProvider', function ($qProvider) {
    $qProvider.errorOnUnhandledRejections(false);
}]);

app.controller('customersCtrl', function($scope, $http) {
  $http.get("https://www.w3schools.com/angular/customers.php").then(function (response) {
      $scope.myData = response.data.records;
  });
});

app.controller('DemoAppController', function($http, $location, $uibModal) {
    const demoApp = this;

    // We identify the node.
    const apiBaseURL = "/api/example/";
    let peers = [];
    let challenges = ["J.P.Morgan Corporate Challenge"];
    let years = ["2015","2016","2017","2018"];

    $http.get(apiBaseURL + "me").then((response) => demoApp.thisNode = response.data.me);

    $http.get(apiBaseURL + "peers").then((response) => peers = response.data.peers);

    demoApp.openModal = () => {
        const modalInstance = $uibModal.open({
            templateUrl: 'demoAppModal.html',
            controller: 'ModalInstanceCtrl',
            controllerAs: 'modalInstance',
            resolve: {
                demoApp: () => demoApp,
                apiBaseURL: () => apiBaseURL,
                peers: () => peers,
                challenges: () => challenges,
                years: () => years
            }
        });

        modalInstance.result.then(() => {}, () => {});
    };

//    demoApp.getIOUs = () => $http.get(apiBaseURL + "ious")
//        .then((response) => demoApp.ious = Object.keys(response.data)
//            .map((key) => response.data[key].state.data)
//            .reverse());
//
//    demoApp.getIOUs();

    demoApp.getResults = () => $http.get(apiBaseURL + "results")
        .then((response) => demoApp.results = Object.keys(response.data)
            .map((key) => response.data[key].state.data)
            .reverse());

    demoApp.getResults();


//
//    jsonData.DocumentResponseResults.forEach(function(Result) {
//      console.log(Result.Name)
//    });



});

app.controller('ModalInstanceCtrl', function ($http, $location, $uibModalInstance, $uibModal, demoApp, apiBaseURL, peers, challenges, years) {
    const modalInstance = this;

    modalInstance.peers = peers;
    modalInstance.challenges = challenges;
    modalInstance.years = years;
    modalInstance.form = {};
    modalInstance.formError = false;

    // Validate and create User Result.
    modalInstance.create = () => {
        if (invalidFormInput()) {
            modalInstance.formError = true;
        } else {
            modalInstance.formError = false;

            $uibModalInstance.close();

            const createResultEndpoint = `${apiBaseURL}add-result?partyName=${modalInstance.form.counterparty}&challengeName=${modalInstance.form.challengeName}&challengeYear=${modalInstance.form.challengeYear}&placeCity=${modalInstance.form.placeCity}&placeGender=${modalInstance.form.placeGender}&bibNumber=${modalInstance.form.bibNumber}&firstName=${modalInstance.form.firstName}&lastName=${modalInstance.form.lastName}&time=${modalInstance.form.time}&gender=${modalInstance.form.gender}`;

            // Create PO and handle success / fail responses.
            $http.put(createResultEndpoint).then(
                (result) => {
                    modalInstance.displayMessage(result);
//                    demoApp.getIOUs();
                    demoApp.getResults();
                },
                (result) => {
                    modalInstance.displayMessage(result);
                }
            );
        }
    };

    modalInstance.displayMessage = (message) => {
        const modalInstanceTwo = $uibModal.open({
            templateUrl: 'messageContent.html',
            controller: 'messageCtrl',
            controllerAs: 'modalInstanceTwo',
            resolve: { message: () => message }
        });

        // No behaviour on close / dismiss.
        modalInstanceTwo.result.then(() => {}, () => {});
    };

    // Close create IOU modal dialogue.
    modalInstance.cancel = () => $uibModalInstance.dismiss();

    // Validate the IOU.
    function invalidFormInput() {
        //TODO Complete validations
        return  (modalInstance.form.counterparty === undefined);
    }
});

// Controller for success/fail modal dialogue.
app.controller('messageCtrl', function ($uibModalInstance, message) {
    const modalInstanceTwo = this;
    modalInstanceTwo.message = message.data;
});