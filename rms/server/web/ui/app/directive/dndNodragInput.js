// Fixed draggable attribute in input element in IE/Edge
// https://github.com/marceljuenemann/angular-drag-and-drop-lists/issues/403
// https://github.com/marceljuenemann/angular-drag-and-drop-lists/issues/242
// https://github.com/marceljuenemann/angular-drag-and-drop-lists/issues/127#issuecomment-295234653

mainApp.directive('dndNodragMouseover', function(){
    return {
        restrict: 'A', 
        require: 'dndNodragMouseover', 
        controller: ['$element', function ( $element ) {
            this.ancestors = [];

            this.findDraggableAncestorsUntilDndDraggable = function ( h ) {
                var a = [];
                while ( h !== null ) {
                    if ( h.attr('draggable') !== undefined ) {
                        a.push({ 
                            element : h, 
                            draggable : h.attr('draggable')
                        });
                    }
                    if ( h.attr('dnd-draggable') !== undefined ) {
                        break;
                    }
                    h = h.parent();
                }
                return a;
            };
            
            this.cleanup = function () {
                this.ancestors = [];
            };
            
            this.removeDraggable = function () {
                this.ancestors = this.findDraggableAncestorsUntilDndDraggable( $element );
                this.ancestors.forEach(function(o){
                    o.element.attr('draggable', 'false');
                });
            };
            
            this.restoreDraggable = function () {
                this.ancestors.forEach(function(o){
                    o.element.attr('draggable', o.draggable);
                });
            };
        }], 

        link: function (scope, iElement, iAttrs, controller) {
            iElement.on('mouseover', function(event){
                controller.removeDraggable();
            });
            iElement.on('mouseout', function(event){
                controller.restoreDraggable();
            });
            scope.$on('$destroy', function(){
                iElement.off('mouseover');
                iElement.off('mouseout');
                controller.cleanup();
            });
        }
    };
});