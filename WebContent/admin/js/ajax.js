function validateCallback(form, callback, confirmMsg) {
	var $form = $(form);
	var formData = new FormData(form);
	
	function onprogress(evt){
		 var loaded = evt.loaded;     //已经上传大小情况 
		 var tot = evt.total;      //附件总大小 
		 var per = Math.floor(100*loaded/tot);  //已经上传的百分比 
	}
	
	if ($.html5Validate.isAllpass($form)) {
		if($form.find('input[type="file"]').length){
			$.ajax({
				type: form.method || 'POST',
				url:$form.attr("action"),
			    cache: false,
			    data: formData,
			    processData: false,
			    contentType: false,
			    xhr: function(){
		  　　　　　　var xhr = $.ajaxSettings.xhr();
		  　　　　　　if(onprogress && xhr.upload) {
		  　　　　　　　　xhr.upload.addEventListener("progress" , onprogress, false);
		  　　　　　　　　return xhr;
		  　　　　　　}
		  　　　　} 
			}).done(function(res) {
				if(res.success){
					var dialog = art.dialog({
					    title: '提示',
					    content: '添加成功',
					    icon: 'succeed',
					    follow: document.getElementById('btn2'),
					    ok: function(){
					    	dialog.close()
					    }
					});
					setTimeout(function(){dialog.close();dialog=null},2000);
				}
			}).fail(function(res) {
				alert(res);
			});   
		}else{
			$.ajax({
				type: form.method || 'POST',
				url:$form.attr("action"),
			    data: formData
			}).done(function(res) {
				if(res.success){
					var dialog = art.dialog({
					    title: '提示',
					    content: '添加成功',
					    icon: 'succeed',
					    follow: document.getElementById('btn2'),
					    ok: function(){
					    	dialog.close()
					    }
					});
					setTimeout(function(){dialog.close();dialog=null},2000);
				}
			}).fail(function(res) {
				alert(res);
			});   
		}
    }
	return false;
}