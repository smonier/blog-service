import {registry} from '@jahia/ui-extender';
import CommentModerationApp from './CommentModeration';

export default function () {
    window.jahia.i18n.loadNamespaces('blog-service');
    
    registry.add('callback', 'blogServiceCommentModeration', {
        targets: ['jahiaApp-init:60'],
        callback: () => {
            registry.add('adminRoute', 'blogServiceCommentModeration', {
                targets: ['jcontent:95'],
                label: 'blog-service:moderation.menuLabel',
                icon: window.jahia.moonstone.toIconComponent('<svg style="width:24px;height:24px" viewBox="0 0 24 24"><path fill="currentColor" d="M9,22A1,1 0 0,1 8,21V18H4A2,2 0 0,1 2,16V4C2,2.89 2.9,2 4,2H20A2,2 0 0,1 22,4V16A2,2 0 0,1 20,18H13.9L10.2,21.71C10,21.9 9.75,22 9.5,22V22H9M10,16V19.08L13.08,16H20V4H4V16H10Z" /></svg>'),
                isSelectable: true,
                requireModuleInstalledOnSite: 'blog-factory',
                render: CommentModerationApp
            });

            console.log('%c Blog Service Comment Moderation registered', 'color: #3c8cba');
        }
    });
}
