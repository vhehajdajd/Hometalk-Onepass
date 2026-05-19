/*
    태그 기능 (추가/삭제/자동완성)
 */

const tagModule = (() => {

    const tagInput = document.querySelector('#tagInput');
    const tagList = document.querySelector('#tag-list');
    const hiddenTags = document.querySelector('#hidden-tags');
    const tagMsgContainer = document.querySelector('#tag-message-container');
    const suggestions = document.getElementById('tagSuggestions');

    let tags = window.initialTags || [];

    function init() {
        if (!tagInput || !tagList || !hiddenTags) {
            return;
        }
        if (tags.length > 0) {
            renderTags();
        }
        bindEvents();
    }

    function bindEvents() {
        tagInput.addEventListener('keyup', handleTagSearch);
        tagInput.addEventListener('keydown', handleTagEnter);
        document.addEventListener('click', handleOutsideClick);
    }

    async function handleTagSearch(e) {
        if (e.key === 'Enter') {
            return;
        }
        const keyword = tagInput.value.trim();
        if (keyword.length < 1) {
            hideSuggestions();
            return;
        }

        try {
            const response = await apiFetch(
                `/hometop/api/resident/tags/search?keyword=${encodeURIComponent(keyword)}`
            );
            const data = await response.json();
            renderSuggestions(data);

        } catch (err) {
            console.error('태그 검색 에러:', err);
        }
    }

    function renderSuggestions(data) {
        if (!suggestions) {
            return;
        }

        if (!data || data.length === 0) {
            hideSuggestions();
            return;
        }

        suggestions.innerHTML = '';

        data.forEach(name => {
            const li = document.createElement('li');
            li.className = 'suggestion-item';
            li.textContent = name;
            li.addEventListener('click', () => {
                selectTag(name);
            });
            suggestions.appendChild(li);
        });
        suggestions.style.display = 'block';
    }

    function hideSuggestions() {
        if (suggestions) {
            suggestions.style.display = 'none';
        }
    }

    function handleOutsideClick(e) {
        if (
            e.target !== tagInput &&
            !suggestions?.contains(e.target)
        ) {
            hideSuggestions();
        }
    }

    function handleTagEnter(e) {
        if (e.key !== 'Enter') {
            return;
        }
        e.preventDefault();
        const tagName = tagInput.value.trim();
        if (!validateTag(tagName)) {
            return;
        }
        if (!tags.includes(tagName)) {
            tags.push(tagName);
            renderTags();
        }
        tagInput.value = '';
        hideSuggestions();
    }

    function validateTag(tagName) {
        if (tagName.length > 5) {
            showTagMessage('태그는 최대 5자까지만 입력 가능합니다.');
            return false;
        }
        if (tags.length >= 5) {
            showTagMessage('태그는 최대 5개까지만 등록 가능합니다.');
            tagInput.value = '';
            return false;
        }
        return true;
    }

    function selectTag(name) {
        if (!validateTag(name)) {
            return;
        }

        if (tags.includes(name)) {
            tagInput.value = '';
            hideSuggestions();
            return;
        }

        tags.push(name);
        renderTags();
        tagInput.value = '';
        hideSuggestions();
        tagInput.focus();
    }

    function renderTags() {
        tagList.innerHTML = '';
        hiddenTags.innerHTML = '';

        tags.forEach((tag, index) => {
            const span = document.createElement('span');
            span.className = 'tag-badge';
            span.textContent = tag;

            const removeBtn = document.createElement('i');
            removeBtn.className = 'remove-tag';
            removeBtn.innerHTML = '&times;';
            removeBtn.addEventListener('click', () => {
                removeTag(index);
            });
            span.appendChild(removeBtn);
            tagList.appendChild(span);
            const input = document.createElement('input');
            input.type = 'hidden';
            input.name = 'tags';
            input.value = tag;
            hiddenTags.appendChild(input);
        });
    }

    function removeTag(index) {
        tags.splice(index, 1);
        renderTags();
    }

    function showTagMessage(message) {
        if (!tagMsgContainer) {
            return;
        }
        tagMsgContainer.innerHTML = `
            <div class="tag-warning-message">
                <span>⚠️</span>
                <span>${message}</span>
            </div>
        `;
        setTimeout(() => {
            tagMsgContainer.innerHTML = '';
        }, 2500);
    }

    return {
        init
    };
})();

document.addEventListener('DOMContentLoaded', () => {
    tagModule.init();
});