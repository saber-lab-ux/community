const productDialog = document.getElementById("productDialog");
const openCreateBtn = document.getElementById("openCreateBtn");
const closeDialogBtn = document.getElementById("closeDialogBtn");
const searchForm = document.getElementById("searchForm");
const searchName = document.getElementById("searchName");
const searchBtn = document.getElementById("searchBtn");
const resetSearchBtn = document.getElementById("resetSearchBtn");

// 当前已经提交的搜索关键词
let currentKeyword = "";

const productList = document.getElementById("productList");
const message = document.getElementById("message");
const pageInfo = document.getElementById("pageInfo");
const prevBtn = document.getElementById("prevBtn");
const nextBtn = document.getElementById("nextBtn");
const refreshBtn = document.getElementById("refreshBtn");

const productForm = document.getElementById("productForm");
const formTitle = document.getElementById("formTitle");
const saveBtn = document.getElementById("saveBtn");
const cancelEditBtn = document.getElementById("cancelEditBtn");
const formMessage = document.getElementById("formMessage");

const nameInput = document.getElementById("productName");
const priceInput = document.getElementById("productPrice");
const stockInput = document.getElementById("productStock");
const statusInput = document.getElementById("productStatus");
const descriptionInput = document.getElementById("productDescription");

let currentPage = 0;
let totalPages = 0;
let loading = false;
let saving = false;
let editingId = null;

const pageSize = 10;

function updateControls() {
    const busy = loading || saving;

    openCreateBtn.disabled = busy;
    closeDialogBtn.disabled = busy;

    searchName.disabled = busy;
    searchBtn.disabled = busy;
    resetSearchBtn.disabled = busy;

    refreshBtn.disabled = busy;
    prevBtn.disabled = busy || currentPage <= 0;
    nextBtn.disabled = busy || currentPage + 1 >= totalPages;

    // 请求进行中暂时锁定表单，避免重复提交
    for (const element of productForm.elements) {
        element.disabled = busy;
    }

    for (const button of productList.querySelectorAll("button")) {
        button.disabled = busy;
    }

    saveBtn.textContent = saving
        ? "正在保存……"
        : editingId === null ? "保存商品" : "保存修改";
}

function addCell(row, value) {
    const cell = document.createElement("td");
    cell.textContent = value ?? "—";
    row.appendChild(cell);
    return cell;
}

function startEdit(product) {
    if (loading || saving) return;

    // 当前下拉框支持这两种状态，避免意外覆盖其他状态
    if (!["ON_SALE", "OFF_SALE"].includes(product.status)) {
        message.className = "error";
        message.textContent = "该商品的状态暂不支持编辑。";
        return;
    }

    editingId = product.id;

    formTitle.textContent = `编辑商品 #${product.id}`;
    nameInput.value = product.name ?? "";
    priceInput.value = product.price ?? "";
    stockInput.value = product.stock ?? "";
    statusInput.value = product.status;
    descriptionInput.value = product.description ?? "";

    cancelEditBtn.hidden = false;
    formMessage.className = "";
    formMessage.textContent = "修改完成后，点击“保存修改”。";

    updateControls();

    showProductDialog();
}

function renderProducts(products) {
    productList.replaceChildren();

    for (const product of products) {
        const row = document.createElement("tr");

        addCell(row, product.id);
        addCell(row, product.name);

        const price = Number(product.price);
        addCell(
            row,
            product.price != null && Number.isFinite(price)
                ? `¥${price.toFixed(2)}`
                : "—"
        );

        addCell(row, product.stock);

        const statusCell = addCell(row, "");
        const badge = document.createElement("span");
        badge.className = "badge";

        if (product.status === "ON_SALE") {
            badge.textContent = "在售";
            badge.classList.add("on-sale");
        } else if (product.status === "OFF_SALE") {
            badge.textContent = "已下架";
        } else {
            badge.textContent = product.status ?? "未知";
        }

        statusCell.appendChild(badge);

        // 每一行的编辑按钮
        const actionCell = addCell(row, "");
        const editBtn = document.createElement("button");
        editBtn.type = "button";
        editBtn.textContent = "编辑";
        editBtn.addEventListener("click", () => startEdit(product));

        actionCell.appendChild(editBtn);

        const deleteBtn = document.createElement("button");
        deleteBtn.type = "button";
        deleteBtn.textContent = "删除";
        deleteBtn.style.marginLeft = "8px";
        deleteBtn.style.color = "#c62828";

        deleteBtn.addEventListener("click", () => deleteProduct(product));

        actionCell.appendChild(deleteBtn);
        productList.appendChild(row);
    }
}

async function loadProducts(page) {
    if (loading) return;

    loading = true;
    updateControls();

    message.className = "";
    message.textContent = "正在加载商品……";

    try {
        const params = new URLSearchParams({
            page: String(page),
            size: String(pageSize),
            name: currentKeyword
        });

        const response = await fetch(
            `/api/products/page?${params.toString()}`
        );

        if (!response.ok) {
            throw new Error(`接口返回 HTTP ${response.status}`);
        }

        const data = await response.json();

        if (!Array.isArray(data.content)) {
            throw new Error("接口返回的商品列表格式不正确");
        }

        currentPage = data.page;
        totalPages = data.totalPages;

        renderProducts(data.content);

        pageInfo.textContent = totalPages === 0
            ? "共 0 条商品"
            : `第 ${currentPage + 1} / ${totalPages} 页，共 ${data.totalElements} 条商品`;

        if (data.content.length === 0) {
            message.textContent = currentKeyword
                ? `没有找到名称包含“${currentKeyword}”的商品`
                : "当前页没有商品";
        } else {
            message.textContent = currentKeyword
                ? `当前搜索：${currentKeyword}`
                : "";
        }
    } catch (error) {
        message.className = "error";
        message.textContent = `列表加载失败：${error.message}。请点击刷新列表重试。`;
    } finally {
        loading = false;
        updateControls();
    }
}

// 清空表单或取消编辑后，恢复为新增模式
productForm.addEventListener("reset", () => {
    editingId = null;
    formTitle.textContent = "新增商品";
    cancelEditBtn.hidden = true;
    formMessage.className = "";
    formMessage.textContent = "";
    updateControls();
});

cancelEditBtn.addEventListener("click", () => {
    closeProductDialog();
});

productForm.addEventListener("submit", async (event) => {
    event.preventDefault();

    if (saving || loading) return;

    const name = nameInput.value.trim();
    const price = Number(priceInput.value);
    const stock = Number(stockInput.value);

    formMessage.className = "";
    formMessage.textContent = "";

    if (!name) {
        formMessage.className = "error";
        formMessage.textContent = "商品名称不能只包含空格。";
        return;
    }

    if (!Number.isFinite(price) || price <= 0) {
        formMessage.className = "error";
        formMessage.textContent = "请输入大于 0 的价格。";
        return;
    }

    if (!Number.isSafeInteger(stock) || stock < 0) {
        formMessage.className = "error";
        formMessage.textContent = "库存必须是大于或等于 0 的整数。";
        return;
    }

    const product = {
        name: name,
        description: descriptionInput.value.trim(),
        price: price,
        stock: stock,
        status: statusInput.value
    };

    // 有编辑 ID 时修改商品，否则新增商品
    const isEditing = editingId !== null;
    const url = isEditing
        ? `/api/products/${editingId}`
        : "/api/products";
    const method = isEditing ? "PUT" : "POST";
    const pageToReload = isEditing ? currentPage : 0;

    saving = true;
    updateControls();

    try {
        const response = await fetch(url, {
            method: method,
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(product)
        });

        if (!response.ok) {
            let detail = `HTTP ${response.status}`;
            const errorBody = await response.json().catch(() => null);

            if (errorBody && typeof errorBody === "object") {
                const messages = Object.values(errorBody)
                    .filter(value => typeof value === "string");

                if (messages.length > 0) {
                    detail = messages.join("；");
                }
            }

            throw new Error(detail);
        }

        const successText = isEditing
            ? "商品修改成功！"
            : "商品保存成功！";

// 保存已完成，关闭弹窗；列表刷新期间仍保持忙碌状态
        productDialog.close();

        await loadProducts(pageToReload);

// 如果列表刷新失败，保留加载失败提示
        if (!message.classList.contains("error")) {
            message.textContent = successText;
        }
    } catch (error) {
        formMessage.className = "error";
        formMessage.textContent =
            `保存未确认成功：${error.message}。如遇网络错误，请先刷新列表确认，再决定是否重试。`;
    } finally {
        saving = false;
        updateControls();
    }
});

prevBtn.addEventListener("click", () => {
    if (currentPage > 0) {
        loadProducts(currentPage - 1);
    }
});

nextBtn.addEventListener("click", () => {
    if (currentPage + 1 < totalPages) {
        loadProducts(currentPage + 1);
    }
});

refreshBtn.addEventListener("click", () => {
    loadProducts(currentPage);
});

loadProducts(0);

async function deleteProduct(product) {
    if (loading || saving) return;

    const confirmed = window.confirm(
        `确定删除商品“${product.name}”（ID：${product.id}）吗？\n删除后无法通过此页面恢复。`
    );

    if (!confirmed) return;

    // 沿用忙碌状态，删除期间禁止重复操作
    saving = true;
    updateControls();
    saveBtn.textContent = "正在删除……";

    message.className = "";
    message.textContent = "正在删除商品……";

    let deleted = false;

    try {
        const response = await fetch(
            `/api/products/${product.id}`,
            { method: "DELETE" }
        );

        if (!response.ok) {
            if (response.status === 404) {
                throw new Error("该商品已不存在，请刷新列表");
            }

            throw new Error(`接口返回 HTTP ${response.status}`);
        }

        deleted = true;

        // 如果删除的是正在编辑的商品，退出编辑模式
        if (editingId === product.id) {
            productForm.reset();
        }

        // 当前页只有这一条商品时，删除后返回上一页
        const pageToReload =
            productList.children.length === 1 && currentPage > 0
                ? currentPage - 1
                : currentPage;

        await loadProducts(pageToReload);

        // 保留列表刷新失败时的错误提示
        if (!message.classList.contains("error")) {
            message.textContent = `已删除商品“${product.name}”。`;
        }
    } catch (error) {
        message.className = "error";
        message.textContent = deleted
            ? `商品已删除，但页面更新失败：${error.message}。请刷新列表。`
            : `删除未确认成功：${error.message}。请刷新列表确认商品是否仍然存在。`;
    } finally {
        saving = false;
        updateControls();
    }
}

searchForm.addEventListener("submit", (event) => {
    event.preventDefault();

    if (loading || saving) return;

    currentKeyword = searchName.value.trim();

    // 每次搜索都从第一页开始
    loadProducts(0);
});

resetSearchBtn.addEventListener("click", () => {
    if (loading || saving) return;

    searchName.value = "";
    currentKeyword = "";

    // 恢复全部商品
    loadProducts(0);
});

function showProductDialog() {
    if (!productDialog.open) {
        productDialog.showModal();
    }

    document.body.classList.add("dialog-open");
    nameInput.focus({ preventScroll: true });
}

function closeProductDialog() {
    // 请求进行中，暂时禁止关闭
    if (saving || loading) return;

    productDialog.close();
}

openCreateBtn.addEventListener("click", () => {
    if (saving || loading) return;

    // 重置表单，同时恢复为新增模式
    productForm.reset();
    showProductDialog();
});

closeDialogBtn.addEventListener("click", () => {
    closeProductDialog();
});

// 按 Esc 关闭时，也检查是否正在保存
productDialog.addEventListener("cancel", (event) => {
    if (saving || loading) {
        event.preventDefault();
    }
});

// 所有关闭方式统一清理表单和页面滚动状态
productDialog.addEventListener("close", () => {
    document.body.classList.remove("dialog-open");
    productForm.reset();
});