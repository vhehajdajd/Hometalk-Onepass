/* global Quill, ImageResize */
// noinspection JSUnresolvedVariable

/*
    Quill 초기화 / 이미지 업로드 / 붙여넣기&드래그드랍
*/


const QuillEditor = window.Quill;
const ImageResizeModule = window.ImageResize?.default || window.ImageResize;

if (QuillEditor && ImageResizeModule) {
    QuillEditor.register('modules/imageResize', ImageResizeModule);
}

/* ================================================
    Quill 에디터 설정 & 이미지 업로드
================================================ */

function initQuill() {
    const editor = document.getElementById('editor');
    if (!editor) return;

    quill = new Quill('#editor', {
        theme: 'snow',
        modules: {
            toolbar: {
                container: [
                    ['bold', 'italic', 'underline'],
                    [{ align: [] }],
                    [{ size: ['small', false, 'large', 'huge'] }],
                    [{ color: [] }],
                    ['link', 'image']
                ],
                handlers: {
                    image: imageHandler
                }
            },
            imageResize: {
                displaySize: true
            }
        }
    });

    const content = document.getElementById('content')?.value;
    if (content && content.trim() !== '') {
        quill.root.innerHTML = content;
    }

    quill.root.addEventListener('paste', handlePaste, false);
    quill.root.addEventListener('drop', handleDrop, false);
}

function imageHandler() {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = 'image/*';
    input.click();

    input.onchange = async () => {
        const file = input.files?.[0];
        if (!file) return;

        await uploadImageFile(file);
    };
}

// 붙여넣기
function handlePaste(e) {
    const files = e.clipboardData?.files;
    if (!files || files.length === 0) return;

    const imageFiles = [...files].filter(file => file.type.match(/^image\//));
    if (imageFiles.length === 0) return;

    e.preventDefault();

    imageFiles.forEach(file => {
        uploadImageFile(file);
    });
}

// 드래그 드롭
function handleDrop(e) {
    const files = e.dataTransfer?.files;
    if (!files || files.length === 0) return;

    const imageFiles = [...files].filter(file => file.type.match(/^image\//));
    if (imageFiles.length === 0) return;

    e.preventDefault();

    imageFiles.forEach(file => {
        uploadImageFile(file);
    });
}

// 공통 업로드 함수
async function uploadImageFile(file) {
    if (!quill || !file) return;

    const formData = new FormData();
    formData.append('file', file);

    const token = document.querySelector('meta[name="_csrf"]')?.content;
    const header = document.querySelector('meta[name="_csrf_header"]')?.content;

    const headers = {};
    if (token && header) {
        headers[header] = token;
    }

    try {
        const res = await fetch('/hometop/community/image-upload', {
            method: 'POST',
            headers,
            body: formData
        });

        if (!res.ok) {
            throw new Error('이미지 업로드 실패');
        }

        const data = await res.json();

        const finalUrl = data.url?.startsWith('/hometop/uploads/')
            ? data.url
            : '/hometop' + data.url;

        const range = quill.getSelection(true) || {
            index: quill.getLength()
        };

        quill.insertEmbed(range.index, 'image', finalUrl);
        quill.setSelection(range.index + 1);

    } catch (err) {
        console.error('이미지 업로드 실패:', err);
        showAlertModal('이미지 업로드 중 오류가 발생했습니다.');
    }
}