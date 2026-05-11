
let cart=[];

function addProduct(){
 apiGet("/products/"+$("#code").val(),p=>{
  cart.push(p);
  render();
 });
}

function render(){
 $("#cart").empty();
 let t=0;
 cart.forEach(p=>{
  $("#cart").append(`<tr><td>${p.name}</td><td>${p.price}</td></tr>`);
  t+=p.price;
 });
 $("#total").text(t);
}

function checkout(){
 apiPost("/sales",{items:cart},r=>{
  window.open(API+"/sales/ticket/"+r.id);
  cart=[];
  render();
 });
}
