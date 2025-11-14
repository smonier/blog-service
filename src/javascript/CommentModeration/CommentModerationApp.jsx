import React from 'react';
import CommentModeration from './CommentModeration';

const CommentModerationApp = ({context}) => {
    // Get site key and lang from context, URL params, or global context
    const siteKey = context?.site || 
                    new URLSearchParams(window.location.search).get('siteName') || 
                    window.contextJsParameters?.siteKey || 
                    'Unknown Site';
    const lang = context?.uilang || 
                 new URLSearchParams(window.location.search).get('lang') || 
                 window.contextJsParameters?.uilang || 
                 'en';

    return <CommentModeration siteKey={siteKey} lang={lang} />;
};

export default CommentModerationApp;
