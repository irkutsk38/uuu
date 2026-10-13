// JAICP uses ES5 JavaScript in script blocks. No external dependencies.
function pizzaCopy(value) { return JSON.parse(JSON.stringify(value)); }
function pizzaDecode(value) {
    if (typeof value !== 'string') return value;
    try { return JSON.parse(value); } catch (e) { return value; }
}
function pizzaReset(session) {
    session.order = {topping:null, size:null, dough:null, address:null,
        receipt:{value:'delivery',name:'Доставка'}, payment:null, quantity:1,
        deliveryTime:null, phone:null, price:0, status:'draft'};
    session.pendingField = null;
}
function pizzaEnsure(session) { if (!session.order) pizzaReset(session); }
function pizzaReprice(session) {
    var o = session.order;
    o.price = o.topping && o.size ? Math.round(o.topping.base_price * o.size.price_modifier * o.quantity) : 0;
}
function pizzaApply(session, tree, entities, query) {
    pizzaEnsure(session);
    tree = tree || {}; entities = entities || []; query = String(query || '');
    var mapping = {PizzaTopping:'topping',PizzaSize:'size',PizzaDough:'dough',PizzaAddress:'address',
        ReceiptMethod:'receipt',PaymentMethod:'payment'};
    var slots = {topping:'topping',size:'size',dough:'dough',address:'address',receipt:'receipt',
        payment:'payment',quantity:'quantity',deliveryTime:'deliveryTime',phone:'phone'};
    var candidates = {}, errors = [], changed = [];
    var i, key, ent, field, val;
    for (i = 0; i < entities.length; i++) {
        ent = entities[i]; field = mapping[ent.pattern];
        if (!field) continue;
        val = pizzaDecode(ent.value);
        if (val === '' || val === null || typeof val === 'undefined') continue;
        if (!candidates[field]) candidates[field] = [];
        candidates[field].push(val);
    }
    for (key in slots) {
        if (!Object.prototype.hasOwnProperty.call(slots,key)) continue;
        val = tree['_' + key];
        if (val !== null && typeof val !== 'undefined' && val !== '' && !candidates[key]) candidates[key] = [pizzaDecode(val)];
    }
    // Legacy slots from the supplied archive also remain usable.
    var legacy = {ToppingSlotOfToppingIntent:'topping',SizeSlotOfSizeIntent:'size',AddressSlotOfAddressIntent:'address'};
    for (key in legacy) {
        val = tree['_' + key];
        if (val && !candidates[legacy[key]]) candidates[legacy[key]] = [pizzaDecode(val)];
    }
    for (field in candidates) {
        if (!Object.prototype.hasOwnProperty.call(candidates,field)) continue;
        var list = candidates[field], unique = [], seen = {};
        for (i = 0; i < list.length; i++) {
            key = JSON.stringify(list[i]);
            if (!seen[key]) { unique.push(list[i]); seen[key] = true; }
        }
        if (unique.length > 1) {
            // Explicit correction "не большую, а маленькую": later entity wins.
            if (/не\s.+[, ]а\s/i.test(query)) unique = [unique[unique.length-1]];
            else { errors.push('Укажите один вариант для параметра ' + field + '.'); continue; }
        }
        val = unique[0];
        if (field === 'quantity') {
            // A house number, diameter or clock hour is not an order quantity.
            if (!/(количеств|штук|пицц|порци)/i.test(query) && session.pendingField !== 'quantity') continue;
            if (candidates.address && !/(количеств|штук)/i.test(query)) continue;
            if (/(\d\s*см|сантиметр)/i.test(query) && !/(количеств|штук)/i.test(query)) continue;
            val = Number(val && typeof val === 'object' ? val.value : val);
            if (!isFinite(val) || val < 1 || val > 10 || Math.floor(val) !== val) {
                errors.push('Количество должно быть целым числом от 1 до 10.'); continue;
            }
        }
        if (field === 'address') {
            val = typeof val === 'object' ? (val.address || val.name || val.value) : val;
            if (typeof val !== 'string' || !val.trim()) { errors.push('Укажите улицу и номер дома.'); continue; }
        }
        if (field === 'topping' || field === 'size' || field === 'dough') {
            if (!val || typeof val !== 'object' || !(val.name || val.ru_name)) {
                errors.push('Не удалось прочитать JSON DATA параметра ' + field + '.'); continue;
            }
            if (!val.name) val.name = val.ru_name;
        }
        if (JSON.stringify(session.order[field]) !== JSON.stringify(val)) changed.push(field);
        session.order[field] = pizzaCopy(val);
    }
    if (changed.length) { session.order.status = 'draft'; session.pendingField = null; }
    pizzaReprice(session);
    return {changed:changed, errors:errors};
}
function pizzaNext(session) {
    pizzaEnsure(session);
    var o = session.order;
    if (!o.topping) return '/AskTopping';
    if (!o.size) return '/AskSize';
    if (!o.dough) return '/AskDough';
    if ((!o.receipt || o.receipt.value !== 'pickup') && !o.address) return '/AskAddress';
    return '/ConfirmOrder';
}
function pizzaShowValue(value) {
    if (value === null || typeof value === 'undefined') return 'не указано';
    if (typeof value === 'object') return String(value.name || value.ru_name || value.value || JSON.stringify(value));
    return String(value);
}
function pizzaSummary(session) {
    pizzaEnsure(session); pizzaReprice(session);
    var o = session.order;
    return 'Проверьте заказ: ' + pizzaShowValue(o.topping) + '; размер: ' + pizzaShowValue(o.size) +
        '; основа: ' + pizzaShowValue(o.dough) + '; количество: ' + o.quantity +
        '; получение: ' + pizzaShowValue(o.receipt) +
        (o.receipt && o.receipt.value === 'pickup' ? '' : '; адрес: ' + pizzaShowValue(o.address)) +
        (o.payment ? '; оплата: ' + pizzaShowValue(o.payment) : '') +
        (o.deliveryTime ? '; время: ' + pizzaShowValue(o.deliveryTime) : '') +
        (o.phone ? '; телефон: ' + pizzaShowValue(o.phone) : '') + '; сумма: ' + o.price + ' руб.';
}
function pizzaConfirm(session) {
    pizzaEnsure(session);
    var next = pizzaNext(session);
    if (next !== '/ConfirmOrder') return {accepted:false, next:next, reason:'missing'};
    if (session.order.status === 'confirmed') return {accepted:false, next:'/AfterOrder', reason:'duplicate'};
    session.order.status = 'confirmed';
    session.orderNumber = (session.orderNumber || 0) + 1;
    session.lastOrder = pizzaCopy(session.order);
    session.lastOrder.number = session.orderNumber;
    return {accepted:true, next:'/AfterOrder', number:session.orderNumber};
}
