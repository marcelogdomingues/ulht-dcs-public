// Internationalization (i18n) Module for ULHT DCS Documentation

class I18nManager {
    constructor() {
        this.currentLanguage = localStorage.getItem('language') || 'en';
        this.translations = {};
        this.init();
    }

    init() {
        this.loadTranslations();
        this.setLanguage(this.currentLanguage);
        this.createLanguageSwitcher();
        this.translatePage();
    }

    loadTranslations() {
        this.translations = {
            en: {
                // Navigation
                'nav.home': 'Home',
                'nav.api-reference': 'API Reference',
                'nav.monitoring': 'Monitoring',
                'nav.architecture': 'Architecture',
                'nav.getting-started': 'Getting Started',
                'nav.search': 'Search',
                'nav.theme-toggle': 'Toggle theme',
                'nav.search-placeholder': 'Search documentation... (Ctrl+K)',
                'nav.search-shortcuts.navigate': '↑↓ Navigate',
                'nav.search-shortcuts.select': 'Enter Select',
                'nav.search-shortcuts.close': 'Esc Close',
                'nav.no-results': 'No results found for',
                'nav.back-to-top': 'Back to top',
                'nav.toc-title': '📋 Table of Contents',
                'nav.breadcrumb.home': 'Home',

                // Common
                'common.loading': 'Loading...',
                'common.copy': 'Copy',
                'common.copied': 'Copied!',
                'common.read-more': 'Read More',
                'common.learn-more': 'Learn More',
                'common.view-docs': 'View Documentation',
                'common.download': 'Download',
                'common.install': 'Install',
                'common.configure': 'Configure',
                'common.deploy': 'Deploy',
                'common.monitor': 'Monitor',
                'common.troubleshoot': 'Troubleshoot',

                // Home page
                'home.hero.title': 'ULHT Digital Credential Service',
                'home.hero.subtitle': 'A comprehensive digital credential management platform for educational institutions',
                'home.hero.description': 'Streamline credential issuance, verification, and management with our secure, scalable, and standards-compliant solution.',
                'home.hero.get-started': 'Get Started',
                'home.hero.view-docs': 'View Documentation',
                'home.overview.title': 'System Overview',
                'home.overview.description': 'The ULHT Digital Credential Service is a modern, cloud-native platform designed to handle the complete lifecycle of digital credentials.',
                'home.features.title': 'Key Features',
                'home.features.secure.title': '🔒 Secure & Compliant',
                'home.features.secure.description': 'Built with enterprise-grade security and W3C Verifiable Credentials standards compliance.',
                'home.features.scalable.title': '📈 Scalable Architecture',
                'home.features.scalable.description': 'Microservices-based architecture that scales with your institution\'s needs.',
                'home.features.integration.title': '🔗 Easy Integration',
                'home.features.integration.description': 'RESTful APIs and comprehensive SDKs for seamless integration.',
                'home.features.monitoring.title': '📊 Advanced Monitoring',
                'home.features.monitoring.description': 'Real-time monitoring, alerting, and analytics for operational excellence.',
                'home.quick-start.title': 'Quick Start',
                'home.quick-start.description': 'Get up and running with the ULHT Digital Credential Service in minutes.',
                'home.documentation.title': 'Documentation',
                'home.documentation.description': 'Comprehensive guides, API references, and examples to help you succeed.',

                // API Reference
                'api.auth.title': 'Authentication',
                'api.auth.description': 'The ULHT Digital Credential Service uses OAuth 2.0 with JWT tokens for secure API access. All API requests must include a valid authentication token.',
                'api.endpoints.title': 'API Endpoints',
                'api.endpoints.description': 'Complete reference for all available API endpoints, including request/response formats and examples.',
                'api.examples.title': 'Code Examples',
                'api.examples.description': 'Practical examples in multiple programming languages to help you integrate quickly.',
                'api.error-codes.title': 'Error Codes',
                'api.error-codes.description': 'Comprehensive list of error codes and their meanings for effective troubleshooting.',

                // Monitoring
                'monitoring.overview.title': 'Monitoring Overview',
                'monitoring.overview.description': 'The ULHT Digital Credential Service includes a comprehensive monitoring stack built with industry-standard tools for observability, alerting, and troubleshooting.',
                'monitoring.metrics.title': 'Metrics Collection',
                'monitoring.metrics.description': 'Prometheus-based metrics collection with custom business metrics and application performance indicators.',
                'monitoring.visualization.title': 'Visualization',
                'monitoring.visualization.description': 'Grafana dashboards for real-time monitoring, historical analysis, and custom alerting rules.',
                'monitoring.logging.title': 'Logging',
                'monitoring.logging.description': 'Centralized logging with Loki for structured log collection, search, and analysis across all services.',
                'monitoring.stack.title': 'Monitoring Stack',
                'monitoring.stack.description': 'Our monitoring solution is built on proven open-source technologies that provide enterprise-grade observability.',
                'monitoring.key-metrics.title': 'Key Metrics',
                'monitoring.key-metrics.description': 'Monitor these critical metrics to ensure optimal performance and identify potential issues early.',
                'monitoring.dashboards.title': 'Grafana Dashboards',
                'monitoring.dashboards.description': 'Pre-configured dashboards provide comprehensive visibility into system performance and business metrics.',
                'monitoring.alerting.title': 'Alerting Configuration',
                'monitoring.alerting.description': 'Configure alerts to be notified of critical issues before they impact users.',

                // Architecture
                'architecture.overview.title': 'System Overview',
                'architecture.overview.description': 'The ULHT Digital Credential Service follows a modern microservices architecture designed for scalability, reliability, and maintainability.',
                'architecture.components.title': 'Core Components',
                'architecture.components.description': 'The system consists of several key components that work together to provide a complete digital credential solution.',
                'architecture.data-flow.title': 'Data Flow',
                'architecture.data-flow.description': 'Understanding how data flows through the system is crucial for integration and troubleshooting.',
                'architecture.security.title': 'Security Architecture',
                'architecture.security.description': 'Multi-layered security approach ensuring data protection and compliance with educational standards.',

                // Getting Started
                'getting-started.prerequisites.title': 'Prerequisites',
                'getting-started.prerequisites.description': 'Before you begin, ensure you have the necessary tools and access to get started with the ULHT Digital Credential Service.',
                'getting-started.installation.title': 'Installation',
                'getting-started.installation.description': 'Step-by-step guide to install and configure the ULHT Digital Credential Service.',
                'getting-started.configuration.title': 'Configuration',
                'getting-started.configuration.description': 'Configure the service to match your institution\'s requirements and security policies.',
                'getting-started.first-credential.title': 'Your First Credential',
                'getting-started.first-credential.description': 'Create and issue your first digital credential using the ULHT Digital Credential Service.',

                // Page titles
                'page.api-reference': 'API Reference',
                'page.monitoring-guide': 'Monitoring Guide',
                'page.architecture': 'Architecture',
                'page.getting-started': 'Getting Started'
            },
            pt: {
                // Navigation
                'nav.home': 'Início',
                'nav.api-reference': 'Referência da API',
                'nav.monitoring': 'Monitorização',
                'nav.architecture': 'Arquitetura',
                'nav.getting-started': 'Começar',
                'nav.search': 'Pesquisar',
                'nav.theme-toggle': 'Alternar tema',
                'nav.search-placeholder': 'Pesquisar documentação... (Ctrl+K)',
                'nav.search-shortcuts.navigate': '↑↓ Navegar',
                'nav.search-shortcuts.select': 'Enter Selecionar',
                'nav.search-shortcuts.close': 'Esc Fechar',
                'nav.no-results': 'Nenhum resultado encontrado para',
                'nav.back-to-top': 'Voltar ao topo',
                'nav.toc-title': '📋 Índice',
                'nav.breadcrumb.home': 'Início',

                // Common
                'common.loading': 'A carregar...',
                'common.copy': 'Copiar',
                'common.copied': 'Copiado!',
                'common.read-more': 'Ler Mais',
                'common.learn-more': 'Saber Mais',
                'common.view-docs': 'Ver Documentação',
                'common.download': 'Transferir',
                'common.install': 'Instalar',
                'common.configure': 'Configurar',
                'common.deploy': 'Implementar',
                'common.monitor': 'Monitorizar',
                'common.troubleshoot': 'Resolver Problemas',

                // Home page
                'home.hero.title': 'Serviço de Credenciais Digitais ULHT',
                'home.hero.subtitle': 'Uma plataforma abrangente de gestão de credenciais digitais para instituições educativas',
                'home.hero.description': 'Simplifique a emissão, verificação e gestão de credenciais com a nossa solução segura, escalável e compatível com padrões.',
                'home.hero.get-started': 'Começar',
                'home.hero.view-docs': 'Ver Documentação',
                'home.overview.title': 'Visão Geral do Sistema',
                'home.overview.description': 'O Serviço de Credenciais Digitais ULHT é uma plataforma moderna, nativa da cloud, concebida para gerir todo o ciclo de vida das credenciais digitais.',
                'home.features.title': 'Características Principais',
                'home.features.secure.title': '🔒 Seguro e Conformante',
                'home.features.secure.description': 'Construído com segurança de nível empresarial e conformidade com os padrões W3C Verifiable Credentials.',
                'home.features.scalable.title': '📈 Arquitetura Escalável',
                'home.features.scalable.description': 'Arquitetura baseada em microserviços que escala com as necessidades da sua instituição.',
                'home.features.integration.title': '🔗 Integração Fácil',
                'home.features.integration.description': 'APIs RESTful e SDKs abrangentes para integração perfeita.',
                'home.features.monitoring.title': '📊 Monitorização Avançada',
                'home.features.monitoring.description': 'Monitorização em tempo real, alertas e análises para excelência operacional.',
                'home.quick-start.title': 'Início Rápido',
                'home.quick-start.description': 'Comece a usar o Serviço de Credenciais Digitais ULHT em minutos.',
                'home.documentation.title': 'Documentação',
                'home.documentation.description': 'Guias abrangentes, referências de API e exemplos para o ajudar a ter sucesso.',

                // API Reference
                'api.auth.title': 'Autenticação',
                'api.auth.description': 'O Serviço de Credenciais Digitais ULHT utiliza OAuth 2.0 com tokens JWT para acesso seguro à API. Todos os pedidos à API devem incluir um token de autenticação válido.',
                'api.endpoints.title': 'Endpoints da API',
                'api.endpoints.description': 'Referência completa para todos os endpoints da API disponíveis, incluindo formatos de pedido/resposta e exemplos.',
                'api.examples.title': 'Exemplos de Código',
                'api.examples.description': 'Exemplos práticos em múltiplas linguagens de programação para o ajudar a integrar rapidamente.',
                'api.error-codes.title': 'Códigos de Erro',
                'api.error-codes.description': 'Lista abrangente de códigos de erro e seus significados para resolução eficaz de problemas.',

                // Monitoring
                'monitoring.overview.title': 'Visão Geral da Monitorização',
                'monitoring.overview.description': 'O Serviço de Credenciais Digitais ULHT inclui uma stack de monitorização abrangente construída com ferramentas padrão da indústria para observabilidade, alertas e resolução de problemas.',
                'monitoring.metrics.title': 'Recolha de Métricas',
                'monitoring.metrics.description': 'Recolha de métricas baseada em Prometheus com métricas de negócio personalizadas e indicadores de desempenho da aplicação.',
                'monitoring.visualization.title': 'Visualização',
                'monitoring.visualization.description': 'Dashboards Grafana para monitorização em tempo real, análise histórica e regras de alerta personalizadas.',
                'monitoring.logging.title': 'Registos',
                'monitoring.logging.description': 'Registos centralizados com Loki para recolha, pesquisa e análise de registos estruturados em todos os serviços.',
                'monitoring.stack.title': 'Stack de Monitorização',
                'monitoring.stack.description': 'A nossa solução de monitorização é construída com tecnologias open-source comprovadas que fornecem observabilidade de nível empresarial.',
                'monitoring.key-metrics.title': 'Métricas Principais',
                'monitoring.key-metrics.description': 'Monitore estas métricas críticas para garantir desempenho ótimo e identificar problemas potenciais precocemente.',
                'monitoring.dashboards.title': 'Dashboards Grafana',
                'monitoring.dashboards.description': 'Dashboards pré-configurados fornecem visibilidade abrangente no desempenho do sistema e métricas de negócio.',
                'monitoring.alerting.title': 'Configuração de Alertas',
                'monitoring.alerting.description': 'Configure alertas para ser notificado de problemas críticos antes de impactarem os utilizadores.',

                // Architecture
                'architecture.overview.title': 'Visão Geral do Sistema',
                'architecture.overview.description': 'O Serviço de Credenciais Digitais ULHT segue uma arquitetura moderna de microserviços concebida para escalabilidade, fiabilidade e manutenibilidade.',
                'architecture.components.title': 'Componentes Principais',
                'architecture.components.description': 'O sistema consiste em vários componentes-chave que trabalham em conjunto para fornecer uma solução completa de credenciais digitais.',
                'architecture.data-flow.title': 'Fluxo de Dados',
                'architecture.data-flow.description': 'Compreender como os dados fluem através do sistema é crucial para integração e resolução de problemas.',
                'architecture.security.title': 'Arquitetura de Segurança',
                'architecture.security.description': 'Abordagem de segurança multicamada garantindo proteção de dados e conformidade com padrões educativos.',

                // Getting Started
                'getting-started.prerequisites.title': 'Pré-requisitos',
                'getting-started.prerequisites.description': 'Antes de começar, certifique-se de que tem as ferramentas e acesso necessários para começar com o Serviço de Credenciais Digitais ULHT.',
                'getting-started.installation.title': 'Instalação',
                'getting-started.installation.description': 'Guia passo a passo para instalar e configurar o Serviço de Credenciais Digitais ULHT.',
                'getting-started.configuration.title': 'Configuração',
                'getting-started.configuration.description': 'Configure o serviço para corresponder aos requisitos da sua instituição e políticas de segurança.',
                'getting-started.first-credential.title': 'A Sua Primeira Credencial',
                'getting-started.first-credential.description': 'Crie e emita a sua primeira credencial digital usando o Serviço de Credenciais Digitais ULHT.',

                // Page titles
                'page.api-reference': 'Referência da API',
                'page.monitoring-guide': 'Guia de Monitorização',
                'page.architecture': 'Arquitetura',
                'page.getting-started': 'Começar'
            }
        };
    }

    setLanguage(language) {
        this.currentLanguage = language;
        localStorage.setItem('language', language);
        this.translatePage();
        
        // Update language switcher
        const languageSwitcher = document.querySelector('.language-switcher');
        if (languageSwitcher) {
            languageSwitcher.querySelectorAll('button').forEach(btn => {
                btn.classList.toggle('active', btn.dataset.lang === language);
            });
        }

        // Update document direction for RTL support
        document.documentElement.setAttribute('dir', language === 'ar' ? 'rtl' : 'ltr');
        document.documentElement.setAttribute('lang', language);
    }

    translatePage() {
        // Translate all elements with data-i18n attribute
        document.querySelectorAll('[data-i18n]').forEach(element => {
            const key = element.getAttribute('data-i18n');
            const translation = this.getTranslation(key);
            if (translation) {
                if (element.tagName === 'INPUT' && element.type === 'placeholder') {
                    element.placeholder = translation;
                } else {
                    element.textContent = translation;
                }
            }
        });

        // Update page title
        const pageTitle = this.getPageTitle();
        if (pageTitle) {
            document.title = pageTitle;
        }

        // Update meta description
        const metaDescription = document.querySelector('meta[name="description"]');
        if (metaDescription) {
            const descriptionKey = this.getDescriptionKey();
            const description = this.getTranslation(descriptionKey);
            if (description) {
                metaDescription.setAttribute('content', description);
            }
        }
    }

    getTranslation(key) {
        return this.translations[this.currentLanguage]?.[key] || this.translations['en'][key] || key;
    }

    getPageTitle() {
        const path = window.location.pathname;
        const pageKey = this.getPageKey(path);
        return this.getTranslation(pageKey);
    }

    getPageKey(path) {
        const pageMap = {
            '/index.html': 'home.hero.title',
            '/api-reference.html': 'page.api-reference',
            '/monitoring-guide.html': 'page.monitoring-guide',
            '/architecture.html': 'page.architecture',
            '/getting-started.html': 'page.getting-started'
        };
        return pageMap[path] || 'home.hero.title';
    }

    getDescriptionKey() {
        const path = window.location.pathname;
        const pageMap = {
            '/index.html': 'home.hero.description',
            '/api-reference.html': 'api.auth.description',
            '/monitoring-guide.html': 'monitoring.overview.description',
            '/architecture.html': 'architecture.overview.description',
            '/getting-started.html': 'getting-started.prerequisites.description'
        };
        return pageMap[path] || 'home.hero.description';
    }

    createLanguageSwitcher() {
        const navMenu = document.querySelector('.nav-menu');
        if (!navMenu) return;

        const languageSwitcher = document.createElement('div');
        languageSwitcher.className = 'language-switcher';
        languageSwitcher.innerHTML = `
            <button class="lang-btn" data-lang="en" title="English">EN</button>
            <button class="lang-btn" data-lang="pt" title="Português">PT</button>
        `;

        // Insert before theme toggle
        const themeToggle = navMenu.querySelector('.theme-toggle');
        if (themeToggle) {
            navMenu.insertBefore(languageSwitcher, themeToggle);
        } else {
            navMenu.appendChild(languageSwitcher);
        }

        // Add event listeners
        languageSwitcher.querySelectorAll('.lang-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                this.setLanguage(btn.dataset.lang);
            });
        });

        // Set initial active state
        this.setLanguage(this.currentLanguage);
    }

    // Method to translate dynamic content
    translateElement(element, key) {
        const translation = this.getTranslation(key);
        if (translation && element) {
            element.textContent = translation;
        }
    }

    // Method to get current language
    getCurrentLanguage() {
        return this.currentLanguage;
    }

    // Method to check if current language is RTL
    isRTL() {
        return this.currentLanguage === 'ar';
    }
}

// Initialize i18n when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    window.i18n = new I18nManager();
});

// Export for use in other modules
if (typeof module !== 'undefined' && module.exports) {
    module.exports = I18nManager;
} 