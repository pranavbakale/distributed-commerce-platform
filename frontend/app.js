const state={products:[],cart:JSON.parse(localStorage.getItem('northstar-cart')||'[]'),filter:'All'};
const $=selector=>document.querySelector(selector);
const money=value=>new Intl.NumberFormat('en-US',{style:'currency',currency:'USD'}).format(Number(value));
const escapeHtml=value=>String(value??'').replace(/[&<>'"]/g,char=>({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#039;','"':'&quot;'}[char]));
const api=async(path,options={})=>{const response=await fetch(path,{headers:{'Content-Type':'application/json',...(options.headers||{})},...options});if(!response.ok){const body=await response.json().catch(()=>({}));throw new Error(body.message||`Request failed (${response.status})`)}return response.status===204?null:response.json()};

function route(){
  const current=(location.hash.slice(1)||'store').split('/')[0];
  document.querySelectorAll('.view').forEach(view=>view.classList.remove('active'));
  document.querySelectorAll('[data-route]').forEach(link=>link.classList.toggle('active',link.dataset.route===current));
  const view=$(`#${current}View`)||$('#storeView');view.classList.add('active');
  if(current==='orders')loadOrders();if(current==='operations')loadOperations();window.scrollTo({top:0,behavior:'smooth'});
}

async function loadProducts(){
  try{state.products=await api('/api/products');renderFilters();renderProducts();renderCart()}
  catch(error){$('#productGrid').innerHTML=`<div class="empty-state"><h3>Catalog unavailable</h3><p>${escapeHtml(error.message)}</p></div>`}
}

function renderFilters(){
  const categories=['All',...new Set(state.products.map(p=>p.category))];
  $('#filters').innerHTML=categories.map(category=>`<button class="${category===state.filter?'active':''}" data-filter="${escapeHtml(category)}">${escapeHtml(category)}</button>`).join('');
  $('#filters').querySelectorAll('button').forEach(button=>button.onclick=()=>{state.filter=button.dataset.filter;renderFilters();renderProducts()});
}

function renderProducts(){
  const products=state.filter==='All'?state.products:state.products.filter(p=>p.category===state.filter);
  $('#productGrid').innerHTML=products.map((product,index)=>`<article class="product-card">
    <div class="product-visual" style="background:${escapeHtml(product.accent)}"><span class="stock-chip">${product.available>0?`${product.available} in stock`:'Sold out'}</span><span class="product-glyph">${String(index+1).padStart(2,'0')}</span></div>
    <div class="product-info"><div><h3>${escapeHtml(product.name)}</h3><p>${escapeHtml(product.category)} · ${escapeHtml(product.sku)}</p></div><strong>${money(product.price)}</strong>
    <button class="add-button" data-sku="${escapeHtml(product.sku)}" ${product.available<1?'disabled':''}><span>${product.available?'Add to bag':'Unavailable'}</span><span>＋</span></button></div>
  </article>`).join('');
  document.querySelectorAll('.add-button').forEach(button=>button.onclick=()=>addToCart(button.dataset.sku));
}

function addToCart(sku){
  const product=state.products.find(p=>p.sku===sku);const existing=state.cart.find(item=>item.sku===sku);
  if(existing){if(existing.quantity>=product.available)return toast('No more stock is available');existing.quantity++}else state.cart.push({sku,quantity:1});
  saveCart();renderCart();toast(`${product.name} added to your bag`);
}
function removeFromCart(sku){state.cart=state.cart.filter(item=>item.sku!==sku);saveCart();renderCart()}
function saveCart(){localStorage.setItem('northstar-cart',JSON.stringify(state.cart))}
function cartDetails(){return state.cart.map(item=>({...item,product:state.products.find(p=>p.sku===item.sku)})).filter(item=>item.product)}
function cartTotal(){return cartDetails().reduce((sum,item)=>sum+Number(item.product.price)*item.quantity,0)}
function renderCart(){
  const details=cartDetails();const count=details.reduce((sum,item)=>sum+item.quantity,0);
  $('#cartCount').textContent=count;$('#drawerCount').textContent=count;$('#cartTotal').textContent=money(cartTotal());$('#checkoutTotal').textContent=money(cartTotal());
  $('#cartEmpty').classList.toggle('hidden',details.length>0);$('#cartItems').classList.toggle('hidden',details.length===0);$('#checkoutButton').disabled=details.length===0;
  $('#cartItems').innerHTML=details.map(({product,quantity})=>`<div class="cart-item"><div class="cart-item-visual" style="background:${escapeHtml(product.accent)}">${escapeHtml(product.name.charAt(0))}</div><div><h3>${escapeHtml(product.name)}</h3><p>Quantity ${quantity}</p><button data-remove="${escapeHtml(product.sku)}">Remove</button></div><strong>${money(Number(product.price)*quantity)}</strong></div>`).join('');
  document.querySelectorAll('[data-remove]').forEach(button=>button.onclick=()=>removeFromCart(button.dataset.remove));
}
function openCart(open=true){$('#cartDrawer').classList.toggle('open',open);$('#scrim').classList.toggle('show',open);$('#cartDrawer').setAttribute('aria-hidden',String(!open))}

async function submitOrder(event){
  event.preventDefault();const button=event.currentTarget.querySelector('button');button.disabled=true;button.firstChild.textContent='Placing order… ';
  const items=cartDetails().map(({product,quantity})=>({sku:product.sku,name:product.name,quantity,unitPrice:product.price}));
  try{await api('/api/orders',{method:'POST',body:JSON.stringify({customerEmail:$('#customerEmail').value,items})});state.cart=[];saveCart();renderCart();$('#checkoutFormView').classList.add('hidden');$('#checkoutSuccess').classList.remove('hidden')}
  catch(error){toast(error.message);button.disabled=false;button.firstChild.textContent='Place order '}
}

async function loadOrders(){
  try{
    const [orders,summary]=await Promise.all([api('/api/orders'),api('/api/orders/metrics/summary')]);
    $('#orderMetrics').innerHTML=[['All orders',summary.total],['In progress',summary.pending],['Completed',summary.completed],['Needs attention',summary.failed]].map(([label,value])=>`<div class="metric"><span>${label}</span><strong>${value}</strong></div>`).join('');
    $('#ordersEmpty').classList.toggle('hidden',orders.length>0);$('.table-wrap').classList.toggle('hidden',orders.length===0);
    $('#ordersTable').innerHTML=orders.map(order=>`<tr><td>#${order.id.slice(0,8)}</td><td>${escapeHtml(order.customerEmail)}</td><td>${new Date(order.createdAt).toLocaleDateString()}</td><td>${money(order.total)}</td><td><span class="status ${order.status}">${order.status.replaceAll('_',' ')}</span></td><td>${canCancel(order.status)?`<button class="quiet-button" data-cancel="${order.id}">Cancel</button>`:''}</td></tr>`).join('');
    document.querySelectorAll('[data-cancel]').forEach(button=>button.onclick=()=>cancelOrder(button.dataset.cancel));
  }catch(error){toast(error.message)}
}
const canCancel=status=>['PENDING_INVENTORY','PAYMENT_PENDING'].includes(status);
async function cancelOrder(id){try{await api(`/api/orders/${id}/cancel`,{method:'POST'});toast('Order cancelled');loadOrders()}catch(error){toast(error.message)}}

async function loadOperations(){
  try{
    const [products,payments]=await Promise.all([api('/api/products'),api('/api/payments')]);
    $('#inventoryList').innerHTML=products.map(product=>`<div class="inventory-row"><div><h3>${escapeHtml(product.name)}</h3><p>${escapeHtml(product.sku)}</p></div><div class="stock-bar"><i style="width:${Math.min(100,product.available/40*100)}%;background:${escapeHtml(product.accent)}"></i></div><strong>${product.available} units</strong></div>`).join('');
    $('#paymentsList').innerHTML=payments.length?payments.slice(0,8).map(payment=>`<div class="payment-row"><div><h3>${escapeHtml(payment.customerEmail)}</h3><p>${payment.status} · #${payment.orderId.slice(0,8)}</p></div><strong>${money(payment.amount)}</strong></div>`).join(''):'<div class="empty-state"><p>No payments captured yet.</p></div>';
  }catch(error){toast(error.message)}
}
function toast(message){const element=$('#toast');element.textContent=message;element.classList.add('show');clearTimeout(window.toastTimer);window.toastTimer=setTimeout(()=>element.classList.remove('show'),2800)}

window.addEventListener('hashchange',route);$('#cartButton').onclick=()=>openCart(true);$('#closeCart').onclick=()=>openCart(false);$('#scrim').onclick=()=>openCart(false);
$('#checkoutButton').onclick=()=>{openCart(false);$('#checkoutFormView').classList.remove('hidden');$('#checkoutSuccess').classList.add('hidden');$('#checkoutDialog').showModal()};
$('#closeCheckout').onclick=()=>$('#checkoutDialog').close();$('#checkoutForm').onsubmit=submitOrder;$('#trackOrder').onclick=()=>$('#checkoutDialog').close();$('#refreshOrders').onclick=loadOrders;
route();loadProducts();setInterval(()=>{if(location.hash==='#orders')loadOrders()},5000);

