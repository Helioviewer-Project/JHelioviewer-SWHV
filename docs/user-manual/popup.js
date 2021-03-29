$( document ).ready(function() {
    $("figure").each(function(index){
        el = $(this).find("img");
        $(this).wrap('<a class="popup" href="'+ el.attr("src") +'"><a>');
    });
    $(".popup").magnificPopup({type:'image'});
});
