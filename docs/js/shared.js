// Shared JavaScript for ULHT DCS Documentation

// Dark mode functionality
class ThemeManager {
    constructor() {
        this.theme = localStorage.getItem('theme') || 'light';
        this.init();
    }

    init() {
        // Set initial theme
        this.setTheme(this.theme);
        
        // Add event listeners
        const themeToggle = document.querySelector('.theme-toggle');
        if (themeToggle) {
            themeToggle.addEventListener('click', () => this.toggleTheme());
        }
    }

    setTheme(theme) {
        document.documentElement.setAttribute('data-theme', theme);
        this.theme = theme;
        localStorage.setItem('theme', theme);
        
        // Update toggle button icon
        const themeToggle = document.querySelector('.theme-toggle');
        if (themeToggle) {
            themeToggle.innerHTML = theme === 'dark' ? '☀️' : '🌙';
        }
    }

    toggleTheme() {
        const newTheme = this.theme === 'light' ? 'dark' : 'light';
        this.setTheme(newTheme);
    }
}

// Global Search functionality
class GlobalSearch {
    constructor() {
        this.searchData = [];
        this.searchModal = null;
        this.searchInput = null;
        this.searchResults = null;
        this.isOpen = false;
        this.init();
    }

    async init() {
        setTimeout(() => {
            const searchBtn = document.querySelector('.search-btn');
            if (searchBtn) {
                searchBtn.onclick = () => this.open();
            }
        }, 50);
        if (!document.querySelector('.search-modal')) {
            this.createSearchModal();
        }
        await this.loadSearchIndex();
        this.setupKeyboardShortcuts();
    }

    async loadSearchIndex() {
        try {
            const res = await fetch('search-index.json');
            this.searchData = await res.json();
        } catch (e) {
            this.searchData = [];
        }
    }

    createSearchModal() {
        // Create search modal (only modal content, not page content)
        this.searchModal = document.createElement('div');
        this.searchModal.className = 'search-modal';
        this.searchModal.innerHTML = `
            <div class="search-overlay"></div>
            <div class="search-container">
                <div class="search-header">
                    <div class="search-input-wrapper">
                        <span class="search-icon">🔍</span>
                        <input type="text" class="search-input" placeholder="Search documentation... (Ctrl+K)">
                        <button class="search-close">✕</button>
                    </div>
                </div>
                <div class="search-results"></div>
                <div class="search-footer">
                    <div class="search-shortcuts">
                        <span>↑↓ Navigate</span>
                        <span>Enter Select</span>
                        <span>Esc Close</span>
                    </div>
                </div>
            </div>
        `;

        document.body.appendChild(this.searchModal);

        // Get references
        this.searchInput = this.searchModal.querySelector('.search-input');
        this.searchResults = this.searchModal.querySelector('.search-results');

        // Add event listeners
        this.searchInput.addEventListener('input', (e) => this.handleSearch(e.target.value));
        this.searchInput.addEventListener('keydown', (e) => this.handleKeydown(e));
        this.searchModal.querySelector('.search-close').addEventListener('click', () => this.close());
        this.searchModal.querySelector('.search-overlay').addEventListener('click', () => this.close());
    }

    handleSearch(query) {
        if (query.length < 2) {
            this.searchResults.innerHTML = '';
            return;
        }
        const results = this.searchData.flatMap(page =>
            page.sections
                .filter(section =>
                    section.title.toLowerCase().includes(query.toLowerCase())
                )
                .map(section => ({
                    ...section,
                    pageTitle: page.pageTitle,
                    pageUrl: section.url
                }))
        );
        this.displayResults(results, query);
    }

    displayResults(results, query) {
        if (results.length === 0) {
            this.searchResults.innerHTML = `
                <div class="search-no-results">
                    <p>No results found for "${query}"</p>
                </div>
            `;
            return;
        }
        const resultsHtml = results.slice(0, 10).map(result => `
            <div class="search-result" data-url="${result.pageUrl}">
                <div class="search-result-title">${this.highlightText(result.title, query)}</div>
                <div class="search-result-page">${result.pageTitle}</div>
                <div class="search-result-snippet">${result.snippet ? this.highlightText(result.snippet, query) : ''}</div>
            </div>
        `).join('');
        this.searchResults.innerHTML = resultsHtml;
        this.searchResults.querySelectorAll('.search-result').forEach(result => {
            result.addEventListener('click', () => {
                const url = result.dataset.url;
                this.close();
                setTimeout(() => {
                  window.location.href = url;
                }, 120);
            });
        });
    }

    highlightText(text, query) {
        const regex = new RegExp(`(${query})`, 'gi');
        return text.replace(regex, '<mark>$1</mark>');
    }

    handleKeydown(e) {
        if (e.key === 'Escape') {
            this.close();
        } else if (e.key === 'Enter') {
            const firstResult = this.searchResults.querySelector('.search-result');
            if (firstResult) {
                const url = firstResult.dataset.url;
                this.close();
                setTimeout(() => {
                  window.location.href = url;
                }, 120);
            }
        }
    }

    open() {
        this.searchModal.classList.add('active');
        this.searchInput.focus();
        this.isOpen = true;
        document.body.style.overflow = 'hidden';
    }

    close() {
        this.searchModal.classList.remove('active');
        this.searchInput.value = '';
        this.searchResults.innerHTML = '';
        this.isOpen = false;
        document.body.style.overflow = '';
    }

    setupKeyboardShortcuts() {
        document.addEventListener('keydown', (e) => {
            if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
                e.preventDefault();
                this.open();
            }
        });
    }
}

// Scroll Spy for Table of Contents
class ScrollSpy {
    constructor() {
        this.toc = document.querySelector('.toc');
        this.headings = [];
        this.observer = null;
        this.init();
    }

    init() {
        if (!this.toc) return;

        this.headings = Array.from(document.querySelectorAll('h2, h3, h4')).filter(h => h.id);
        this.setupIntersectionObserver();
        this.updateTocLinks();
    }

    setupIntersectionObserver() {
        const options = {
            rootMargin: '-20% 0px -80% 0px',
            threshold: 0
        };

        this.observer = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    this.setActiveSection(entry.target.id);
                }
            });
        }, options);

        this.headings.forEach(heading => {
            this.observer.observe(heading);
        });
    }

    setActiveSection(activeId) {
        // Remove active class from all TOC links
        this.toc.querySelectorAll('a').forEach(link => {
            link.classList.remove('active');
        });

        // Add active class to current section
        const activeLink = this.toc.querySelector(`a[href="#${activeId}"]`);
        if (activeLink) {
            activeLink.classList.add('active');
        }
    }

    updateTocLinks() {
        const tocLinks = this.toc.querySelectorAll('a');
        tocLinks.forEach(link => {
            link.addEventListener('click', (e) => {
                e.preventDefault();
                const targetId = link.getAttribute('href').substring(1);
                const targetElement = document.getElementById(targetId);
                if (targetElement) {
                    targetElement.scrollIntoView({ behavior: 'smooth', block: 'start' });
                }
            });
        });
    }
}

// Breadcrumb Navigation
class BreadcrumbNavigation {
    constructor() {
        this.breadcrumbContainer = null;
        this.init();
    }

    init() {
        // Only show breadcrumb if not on homepage
        const path = window.location.pathname;
        if (path.endsWith('/index.html') || path === '/' || path === '/index.htm') {
            return;
        }
        this.createBreadcrumb();
        this.updateBreadcrumb();
    }

    createBreadcrumb() {
        this.breadcrumbContainer = document.createElement('nav');
        this.breadcrumbContainer.className = 'breadcrumb';
        this.breadcrumbContainer.setAttribute('aria-label', 'Breadcrumb');

        // Insert after header
        const header = document.querySelector('header');
        if (header) {
            header.parentNode.insertBefore(this.breadcrumbContainer, header.nextSibling);
        }
    }

    updateBreadcrumb() {
        const path = window.location.pathname;
        const segments = path.split('/').filter(segment => segment);
        
        let breadcrumbHtml = '<ol class="breadcrumb-list">';
        
        // Home link
        breadcrumbHtml += '<li class="breadcrumb-item"><a href="index.html">Home</a></li>';
        
        // Page-specific breadcrumbs
        if (segments.length > 0) {
            const currentPage = segments[segments.length - 1];
            const pageTitle = this.getPageTitle(currentPage);
            
            if (pageTitle && pageTitle !== 'Home') {
                breadcrumbHtml += `<li class="breadcrumb-item"><span>${pageTitle}</span></li>`;
            }
        }
        
        breadcrumbHtml += '</ol>';
        this.breadcrumbContainer.innerHTML = breadcrumbHtml;
    }

    getPageTitle(pageName) {
        const titles = {
            'index.html': 'Home',
            'api-reference.html': 'API Reference',
            'monitoring-guide.html': 'Monitoring Guide',
            'architecture.html': 'Architecture',
            'getting-started.html': 'Getting Started'
        };
        return titles[pageName] || pageName;
    }
}

// Back to Top Button
class BackToTop {
    constructor() {
        this.button = null;
        this.init();
    }

    init() {
        this.createButton();
        this.setupScrollListener();
    }

    createButton() {
        this.button = document.createElement('button');
        this.button.className = 'back-to-top';
        this.button.innerHTML = '↑';
        this.button.setAttribute('aria-label', 'Back to top');
        this.button.title = 'Back to top';
        
        this.button.addEventListener('click', () => {
            window.scrollTo({ top: 0, behavior: 'smooth' });
        });

        document.body.appendChild(this.button);
    }

    setupScrollListener() {
        let isVisible = false;
        
        window.addEventListener('scroll', () => {
            const scrollTop = window.pageYOffset;
            
            if (scrollTop > 300 && !isVisible) {
                this.button.classList.add('visible');
                isVisible = true;
            } else if (scrollTop <= 300 && isVisible) {
                this.button.classList.remove('visible');
                isVisible = false;
            }
        });
    }
}

// Smooth scrolling for anchor links
function initSmoothScrolling() {
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function (e) {
            e.preventDefault();
            const target = document.querySelector(this.getAttribute('href'));
            if (target) {
                target.scrollIntoView({
                    behavior: 'smooth',
                    block: 'start'
                });
            }
        });
    });
}

// Copy to clipboard functionality
function initCopyButtons() {
    document.querySelectorAll('.copy-btn').forEach(button => {
        button.addEventListener('click', function() {
            const codeBlock = this.closest('.code-block');
            const code = codeBlock.textContent;
            
            navigator.clipboard.writeText(code).then(() => {
                // Show success feedback
                const originalText = this.textContent;
                this.textContent = 'Copied!';
                this.style.background = '#48bb78';
                
                setTimeout(() => {
                    this.textContent = originalText;
                    this.style.background = '';
                }, 2000);
            }).catch(err => {
                console.error('Failed to copy: ', err);
            });
        });
    });
}

// Add copy buttons to code blocks
function addCopyButtonsToCodeBlocks() {
    document.querySelectorAll('.code-block').forEach(block => {
        if (!block.querySelector('.copy-btn')) {
            const copyBtn = document.createElement('button');
            copyBtn.className = 'copy-btn';
            copyBtn.textContent = 'Copy';
            copyBtn.style.cssText = `
                position: absolute;
                top: 10px;
                right: 10px;
                background: rgba(255, 255, 255, 0.1);
                border: none;
                color: #e2e8f0;
                padding: 5px 10px;
                border-radius: 4px;
                cursor: pointer;
                font-size: 0.8rem;
                transition: all 0.3s ease;
            `;
            
            copyBtn.addEventListener('mouseenter', () => {
                copyBtn.style.background = 'rgba(255, 255, 255, 0.2)';
            });
            
            copyBtn.addEventListener('mouseleave', () => {
                copyBtn.style.background = 'rgba(255, 255, 255, 0.1)';
            });
            
            block.style.position = 'relative';
            block.appendChild(copyBtn);
        }
    });
}

// Table of contents generation
function generateTOC() {
    const tocContainer = document.querySelector('.sidebar-toc.toc');
    if (!tocContainer) return;
    
    const headings = document.querySelectorAll('h2, h3');
    if (headings.length === 0) {
        tocContainer.style.display = 'none';
        return;
    }
    tocContainer.style.display = '';
    tocContainer.innerHTML = '';
    // TOC Title (i18n)
    const tocTitle = document.createElement('div');
    tocTitle.className = 'toc-title';
    tocTitle.setAttribute('data-i18n', 'nav.toc-title');
    tocTitle.textContent = (window.I18N && typeof window.I18N.getTranslation === 'function') ? window.I18N.getTranslation('nav.toc-title') : '📋 On this page';
    tocContainer.appendChild(tocTitle);
    // TOC List
    const tocList = document.createElement('ul');
    headings.forEach((heading, index) => {
        if (heading.textContent.trim().toLowerCase() === 'table of contents') return;
        if (!heading.id) heading.id = `heading-${index}`;
        const listItem = document.createElement('li');
        const link = document.createElement('a');
        link.href = `#${heading.id}`;
        link.textContent = heading.textContent;
        if (heading.tagName === 'H3') listItem.style.marginLeft = '20px';
        listItem.appendChild(link);
        tocList.appendChild(listItem);
    });
    tocContainer.appendChild(tocList);
}

// Mobile menu toggle
function initMobileMenu() {
    const mobileMenuBtn = document.querySelector('.mobile-menu-btn');
    const navLinks = document.querySelector('.nav-links');
    
    if (mobileMenuBtn && navLinks) {
        mobileMenuBtn.addEventListener('click', () => {
            navLinks.classList.toggle('active');
        });
    }
}

// Search functionality
function initSearch() {
    const searchInput = document.querySelector('.search-input');
    if (!searchInput) return;
    
    searchInput.addEventListener('input', (e) => {
        const query = e.target.value.toLowerCase();
        const content = document.querySelector('.content');
        
        if (query.length < 2) {
            // Show all content
            content.style.display = 'block';
            return;
        }
        
        // Simple search implementation
        const text = content.textContent.toLowerCase();
        if (text.includes(query)) {
            content.style.display = 'block';
            // Highlight search terms (basic implementation)
            highlightText(content, query);
        } else {
            content.style.display = 'none';
        }
    });
}

// Basic text highlighting
function highlightText(element, query) {
    const walker = document.createTreeWalker(
        element,
        NodeFilter.SHOW_TEXT,
        null,
        false
    );
    
    const textNodes = [];
    let node;
    while (node = walker.nextNode()) {
        textNodes.push(node);
    }
    
    textNodes.forEach(textNode => {
        const text = textNode.textContent;
        const regex = new RegExp(`(${query})`, 'gi');
        if (regex.test(text)) {
            const highlightedText = text.replace(regex, '<mark>$1</mark>');
            const span = document.createElement('span');
            span.innerHTML = highlightedText;
            textNode.parentNode.replaceChild(span, textNode);
        }
    });
}

// Progress bar for reading
function initProgressBar() {
    const progressBar = document.createElement('div');
    progressBar.className = 'reading-progress';
    progressBar.style.cssText = `
        position: fixed;
        top: 0;
        left: 0;
        width: 0%;
        height: 3px;
        background: var(--accent-gradient);
        z-index: 1000;
        transition: width 0.3s ease;
    `;
    
    document.body.appendChild(progressBar);
    
    window.addEventListener('scroll', () => {
        const scrollTop = window.pageYOffset;
        const docHeight = document.body.scrollHeight - window.innerHeight;
        const scrollPercent = (scrollTop / docHeight) * 100;
        progressBar.style.width = scrollPercent + '%';
    });
}

// Inject shared components (header, sidebar, footer)
function loadSharedComponents(callback) {
    let loaded = 0;
    const total = 3;
    const inject = (selector, url) => {
        const el = document.getElementById(selector);
        if (el) {
            fetch(url)
                .then(res => res.text())
                .then(html => { el.innerHTML = html; loaded++; if (loaded === total && callback) callback(); })
                .catch(() => { loaded++; if (loaded === total && callback) callback(); });
        } else { loaded++; if (loaded === total && callback) callback(); }
    };
    inject('header', 'header.html');
    inject('sidebar', 'sidebar.html');
    inject('footer', 'footer.html');
}

// Add automatic next/previous navigation links
function addNextPreviousLinks() {
    fetch('search-index.json')
        .then(res => res.json())
        .then(index => {
            const currentPage = window.location.pathname.split('/').pop();
            const pageIdx = index.findIndex(p => p.page === currentPage);
            if (pageIdx === -1) return;
            const prev = index[pageIdx - 1];
            const next = index[pageIdx + 1];
            if (!prev && !next) return;
            const nav = document.createElement('nav');
            nav.className = 'next-prev-nav';
            let html = '';
            if (prev) {
                html += `<a class="prev-link" href="${prev.page}">&larr; <span>${prev.pageTitle}</span></a>`;
            }
            if (next) {
                html += `<a class="next-link" href="${next.page}"><span>${next.pageTitle}</span> &rarr;</a>`;
            }
            nav.innerHTML = html;
            // Insert after main doc content
            const docContent = document.querySelector('.doc-content');
            if (docContent) {
                docContent.appendChild(nav);
            }
        });
}

// Initialize all features when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    loadSharedComponents(() => {
        // Initialize theme manager
        new ThemeManager();
        // Initialize advanced navigation features
        window.globalSearch = new GlobalSearch();
        new ScrollSpy();
        new BreadcrumbNavigation();
        new BackToTop();
        // Initialize other features
        initSmoothScrolling();
        addCopyButtonsToCodeBlocks();
        initCopyButtons();
        generateTOC();
        initMobileMenu();
        initSearch();
        initProgressBar();
        // Re-initialize i18n if present
        if (window.I18N && typeof window.I18N.init === 'function') {
            window.I18N.init();
        }
        // Add loading animation
        document.body.classList.add('loaded');
        // --- Robust scroll to anchor on page load (for search navigation) ---
        if (window.location.hash) {
            let tries = 0;
            const scrollToHash = () => {
                const el = document.getElementById(window.location.hash.substring(1));
                if (el) {
                    el.scrollIntoView({ behavior: 'smooth', block: 'start' });
                } else if (tries < 20) {
                    tries++;
                    setTimeout(scrollToHash, 50);
                }
            };
            setTimeout(scrollToHash, 220);
        }
        addNextPreviousLinks();
    });
});

// Add loading animation styles
const loadingStyles = document.createElement('style');
loadingStyles.textContent = `
    body {
        opacity: 0;
        transition: opacity 0.3s ease;
    }
    
    body.loaded {
        opacity: 1;
    }
    
    .copy-btn:hover {
        background: rgba(255, 255, 255, 0.3) !important;
    }
    
    mark {
        background: #fef08a;
        padding: 2px 4px;
        border-radius: 3px;
    }
    
    [data-theme="dark"] mark {
        background: #fbbf24;
        color: #1f2937;
    }
`;
document.head.appendChild(loadingStyles); 