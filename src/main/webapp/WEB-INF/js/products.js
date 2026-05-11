
function saveProduct(){
 apiPost("/products",{
  name:pname.value,
  price:parseFloat(pprice.value)
 },loadProducts);
}

function loadProducts(){
 apiGet("/products",list=>{
  $("#plist").empty();
  list.forEach(p=>{
   $("#plist").append(`<tr><td>${p.name}</td><td>${p.price}</td></tr>`);
  });
 });
}
loadProducts();
