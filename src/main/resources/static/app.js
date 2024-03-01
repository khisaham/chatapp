var chatApp = angular.module('chatApp',[]);

chatApp.factory('getSockJS', function () {
    return {
        sock: function(){
            var sock = new SockJS('http://localhost:8080/integration');
            return sock;
        }
    }
});

chatApp.controller('chatApp.message', ['$scope', 'getSockJS', '$http', '$rootScope',
    function($scope, getSockJS, $http, $rootScope) {
        var sockjs = getSockJS.sock();

        $http.get("http://localhost:8080/sessions/findAll").then(function(response) {
            $scope.sessions = response.data;
        });


        $scope.messageSend = "";

        $scope.sessionID  = "";

        $scope.sessions = [];

        $scope.messageUser = [];
        $scope.messageFilter = [];
        $scope.userSelected = {};

        sockjs.onopen = function(e) {
            console.log("Connected to socket");
        };

        sockjs.onmessage = function(e) {
            var data = JSON.parse(e.data)
            console.log("MESSAGE = " + JSON.stringify(data))

            switch (data.type){
                case "SESSION_ID":
                    $scope.sessionID = data.body;
                    console.log("SESSION_ID = " + $scope.sessionID)
                    break;
                case "NEW_USER":
                    $scope.sessions.push({
                        id: data.body
                    });
                    break;
                case "NEW_MESSAGE":
                    $scope.messageUser.push(data.body);
                    console.log("Incoming message ")
                    console.log($scope.messageUser);

                    Push.create("Incoming message from " + data.body.id, {
                        body: data.body.message,
                        timeout: 4000,
                        onClick: function () {
                            window.focus();
                            this.close();
                        }
                    });
                    updateMessages();
                    break;
            }

            $scope.$apply();
        };

        sockjs.onclose = function() {
            console.log('close');
        };
        
        $scope.selectSessionSend = function (sesion) {
            console.log(sesion);
            $scope.userSelected = sesion;
            updateMessages();
        }

        $scope.sendMessage = function (message) {
            sockjs.send(JSON.stringify({
                id: $scope.userSelected.id,
                message: message
            }));

            $scope.messageUser.push({
                id: $scope.sessionID,
                message: message,
                date: new Date().toString()
            });

            updateMessages();
        }

        var updateMessages = function () {
            $scope.messageFilter = [];
            console.log($scope.messageUser)

            for (data  in $scope.messageUser){
                console.log(data)
                if($scope.messageUser[data].id == $scope.userSelected.id){
                    $scope.messageFilter.push($scope.messageUser[data]);
                } else if ($scope.messageUser[data].id == $scope.sessionID){
                    $scope.messageFilter.push($scope.messageUser[data]);
                }
            }
            console.log($scope.messageFilter)
        }
    }]);