
function login(){
 apiPost("/auth/login",{
  username:$("#user").val(),
  password:$("#pass").val()
 },res=>{
  localStorage.token=res.token;
  localStorage.role=res.role;
  window.location="index";
 });
}

function requireRole(role){
 if(localStorage.role!==role){
  alert("Acceso denegado");
  window.location="index";
 }
}
