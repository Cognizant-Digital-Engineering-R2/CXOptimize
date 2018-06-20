(function( $ ){
    $.fn.poppy = function(poppyId){
        $poppyScreen = $('<div id="poppyScreen"></div>');
        $poppy = $(".poppy");
        $("body").prepend($poppyScreen);
        $poppy.hide().each(function(){
            $(this).prepend('<div class="close-btn"></div><div class="min-btn"></div>');
        });
        function resize(){
            var l = ($(window).width())/4;
            var w =  l + "px";
            $poppy.css({"left":w});
        }
        function closePoppy(){
            $("#poppyScreen, .poppy").fadeOut();
        }
        $(".close-btn, #poppyScreen").on("click",function(){
            closePoppy();
        });
        resize();
        $(window).resize(function() {
          resize();
        });
        this.on("click",function(){
            $poppy = $("#"+poppyId);
            $poppy.fadeIn();
            $poppyScreen.fadeIn();
        });
    };
})(jQuery);
