
function saveUser(){
 apiPost("/users",{
  username:uuser.value,
  password:upass.value,
  role:urole.value
 },()=>alert("Usuario creado"));
}
