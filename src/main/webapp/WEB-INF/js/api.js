
const API="http://localhost:8080";

function apiGet(url,cb){
 $.get(API+url,cb);
}

function apiPost(url,data,cb){
 $.ajax({
  url:API+url,
  method:"POST",
  contentType:"application/json",
  data:JSON.stringify(data),
  success:cb
 });
}
