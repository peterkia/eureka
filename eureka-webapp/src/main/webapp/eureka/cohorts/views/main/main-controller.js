(function() {
    'use strict';

    /**
     * @ngdoc controller
     * @name eureka.cohorts.controller:MainCtrl
     * @description
     * This is the main controller for the cohorts section of the application.
     * @requires cohorts.CohortService
     */

    angular
        .module('eureka.cohorts')
        .controller('cohorts.MainCtrl', MainCtrl);
        
    MainCtrl.$inject = ['CohortService'];
    
    function MainCtrl(CohortService) {
        var vm = this;
        vm.remove = remove;

        function remove(key) {
            CohortService.removeCohort(key);
            for (var i = 0; i < vm.cohortsList.length; i++) {
                if (vm.cohortsList[i].name === key) {
                    vm.cohortsList.splice(i, 1);
                    break;
                }
            }
        }

        function displayError(msg) {
            vm.errorMsg = msg;
        }

        vm.selected = [];

        vm.filter = {
            options: {
                debounce: 500
            }
        };

        vm.query = {
            filter: '',
            order: 'name',
            limit: 5,
            page: 1
        };

        function success(cohorts) {
            vm.cohortsList = cohorts;
            vm.gridOptions.data = cohorts;
        }

        vm.removeFilter = function () {
            vm.filter.show = false;
            vm.query.filter = '';

            if(vm.filter.form.$dirty) {
                vm.filter.form.$setPristine();
            }
        };

        // in the future we may see a few built in alternate headers but in the mean time
        // you can implement your own search header and do something like
        vm.search = function (predicate) {
            vm.filter = predicate;
            vm.deferred = CohortService.getCohorts(vm.query).then(success, displayError);
        };

        vm.onOrderChange = function () {
            return CohortService.getCohorts(vm.query);
        };

        vm.onPaginationChange = function () {
            return CohortService.getCohorts(vm.query);
        };

        // table options JS
        vm.gridOptions = {
        enableSorting: true,
        columnDefs: [
          { name:'Name', field: 'name' },
          { name:'Descripton', field: 'descripton' },
          { name:'Type', field: 'type'},
          { name:'Created', field: 'created_at', enableCellEdit:false}
        ],
        data: []
      };

        CohortService.getCohorts().then(success, displayError);

     }
})();