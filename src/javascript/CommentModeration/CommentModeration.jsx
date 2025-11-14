import React, {useState, useEffect, useRef, useMemo, useCallback} from 'react';
import {useQuery, useMutation, useLazyQuery} from '@apollo/client';
import {useTranslation} from 'react-i18next';
import {Header, Button, Dropdown} from '@jahia/moonstone';
import {GET_ALL_COMMENTS, GET_POST_BY_ID, UPDATE_COMMENT_STATUS, DELETE_COMMENT} from '~/gql-queries/CommentModeration.gql-queries';
import styles from './CommentModeration.module.scss';

const CommentModeration = ({siteKey, lang}) => {
    console.log('==== CommentModeration RENDER START ====');
    console.log('Props - siteKey:', siteKey, 'lang:', lang);
    
    const {t} = useTranslation('blog-service');
    const [filter, setFilter] = useState('all');

    // GraphQL query to fetch comments
    const {data, loading, error, refetch} = useQuery(GET_ALL_COMMENTS, {
        variables: {lang: lang || 'en'},
        fetchPolicy: 'cache-first',
        notifyOnNetworkStatusChange: false
    });
    
    console.log('Apollo Query State:');
    console.log('  - loading:', loading);
    console.log('  - hasData:', !!data);
    console.log('  - error:', error);
    if (data) {
        console.log('  - nodes count:', data?.jcr?.nodesByCriteria?.nodes?.length || 0);
    }

    // Mutations
    const [updateStatusMutation] = useMutation(UPDATE_COMMENT_STATUS);
    const [deleteCommentMutation] = useMutation(DELETE_COMMENT);
    
    // State for post names
    const [postNames, setPostNames] = useState({});
    const [getPostById] = useLazyQuery(GET_POST_BY_ID);

    // Detect unmounting
    useEffect(() => {
        console.log('✅ CommentModeration MOUNTED');
        return () => {
            console.log('❌ CommentModeration UNMOUNTING - this should not happen unexpectedly!');
        };
    }, []);

    const safeFormatDateTime = (value) => {
        if (!value) {
            return {date: '', time: ''};
        }
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return {date: '', time: ''};
        }
        return {
            date: date.toLocaleDateString(),
            time: date.toLocaleTimeString()
        };
    };

    // Process comments data
    const comments = useMemo(() => {
        console.log('useMemo: Processing comments...');
        try {
            if (!data?.jcr?.nodesByCriteria?.nodes) {
                console.log('  -> No data, returning empty array');
                return [];
            }
            
            console.log('  -> Processing', data.jcr.nodesByCriteria.nodes.length, 'nodes');
            const processed = data.jcr.nodesByCriteria.nodes.map(node => {
                // Extract postId from path: /sites/{site}/contents/ugc/blogs/{postId}/comments/{commentId}
                let postId = '';
                if (node.path) {
                    const pathParts = node.path.split('/');
                    const blogsIndex = pathParts.indexOf('blogs');
                    if (blogsIndex !== -1 && pathParts.length > blogsIndex + 1) {
                        postId = pathParts[blogsIndex + 1];
                    }
                }
                
                // Determine status: use status property if available, otherwise derive from approved boolean
                let status = node.status?.value;
                if (!status) {
                    // Fallback for old comments without status property
                    const approved = node.approved?.value;
                    status = approved === 'true' || approved === true ? 'approved' : 'pending';
                }
                
                return {
                    uuid: node.uuid,
                    author: node.author?.value || 'Anonymous',
                    body: node.comment?.value || '',
                    status: status,
                    created: node.created?.value || new Date().toISOString(),
                    postId: postId
                };
            });
            console.log('  -> Processed comments:', processed.length);
            return processed;
        } catch (err) {
            console.error('ERROR processing comments:', err);
            return [];
        }
    }, [data]);    // Calculate stats synchronously based on comments (not in useEffect!)
    const stats = useMemo(() => {
        console.log('useMemo: Calculating stats from', comments.length, 'comments');
        const calculated = {
            total: comments.length,
            pending: comments.filter(c => c.status === 'pending').length,
            approved: comments.filter(c => c.status === 'approved').length,
            rejected: comments.filter(c => c.status === 'rejected').length
        };
        console.log('  -> Stats:', calculated);
        return calculated;
    }, [comments]);

    const updateCommentStatus = useCallback(async (commentId, status) => {
        try {
            await updateStatusMutation({
                variables: {
                    commentId,
                    status,
                    approved: status === 'approved' ? 'true' : 'false'
                },
                refetchQueries: ['GetAllComments']
            });
        } catch (err) {
            console.error('Error updating comment:', err);
            alert('Error updating comment: ' + err.message);
        }
    }, [updateStatusMutation]);

    const deleteComment = useCallback(async (commentId) => {
        if (!window.confirm('Are you sure you want to delete this comment? This action cannot be undone.')) {
            return;
        }

        try {
            await deleteCommentMutation({
                variables: {commentId},
                refetchQueries: ['GetAllComments']
            });
        } catch (err) {
            console.error('Error deleting comment:', err);
            alert('Error deleting comment: ' + err.message);
        }
    }, [deleteCommentMutation]);

    const handleRefresh = useCallback(() => {
        refetch();
    }, [refetch]);
    
    // Fetch post names for all unique posts
    useEffect(() => {
        if (!comments || comments.length === 0) {
            return;
        }
        
        const uniquePostIds = [...new Set(comments.map(c => c.postId).filter(Boolean))];
        
        uniquePostIds.forEach(async (postId) => {
            if (!postNames[postId]) {
                try {
                    const {data: postData} = await getPostById({
                        variables: {postId, lang: lang || 'en'}
                    });
                    if (postData?.jcr?.nodeById?.displayName) {
                        setPostNames(prev => ({
                            ...prev,
                            [postId]: postData.jcr.nodeById.displayName
                        }));
                    }
                } catch (err) {
                    console.error('Error fetching post name for', postId, err);
                }
            }
        });
    }, [comments, getPostById, lang, postNames]);

    const setFilterAll = useCallback(() => setFilter('all'), []);
    const setFilterPending = useCallback(() => setFilter('pending'), []);
    const setFilterApproved = useCallback(() => setFilter('approved'), []);
    const setFilterRejected = useCallback(() => setFilter('rejected'), []);

    const filteredComments = useMemo(() => {
        console.log('useMemo: Filtering comments, filter:', filter, 'total comments:', comments.length);
        if (filter === 'all') {
            console.log('  -> Returning all', comments.length, 'comments');
            return comments;
        }
        const filtered = comments.filter(comment => comment.status === filter);
        console.log('  -> Filtered to', filtered.length, 'comments with status:', filter);
        return filtered;
    }, [comments, filter]);
    
    // Group comments by post (must come after filteredComments is defined)
    const groupedComments = useMemo(() => {
        const groups = {};
        filteredComments.forEach(comment => {
            const postId = comment.postId || 'unknown';
            if (!groups[postId]) {
                groups[postId] = [];
            }
            groups[postId].push(comment);
        });
        return groups;
    }, [filteredComments]);

    console.log('Current State BEFORE RENDER:');
    console.log('  - stats:', stats);
    console.log('  - filter:', filter);
    console.log('  - comments.length:', comments.length);
    console.log('  - filteredComments.length:', filteredComments.length);
    console.log('  - Will show loading?', loading && !data);
    console.log('==== About to render JSX ====');

    try {
        console.log('TRY: Starting JSX render...');
        const jsx = (
        <div style={{display: 'flex', flexDirection: 'column', height: '100vh', overflow: 'hidden'}}>
            <Header
                title={t('moderation.title', {siteInfo: siteKey})}
                mainActions={[
                    <Button
                        key="refreshButton"
                        size="big"
                        color="accent"
                        label={t('moderation.refresh')}
                        onClick={handleRefresh}
                    />
                ]}
            />
            
            <div style={{padding: '20px', fontFamily: 'Arial, sans-serif', flex: 1, overflow: 'auto'}}>
                {loading && !data ? (
                    <div style={{textAlign: 'center', fontSize: '16px', color: '#6c757d', padding: '60px 20px', background: '#f8f9fa', borderRadius: '8px'}}>
                        {t('moderation.loading')}
                    </div>
                ) : error ? (
                    <p style={{color: 'red'}}>{t('moderation.error')}: {error.message}</p>
                ) : (
                    <div>
                        <div style={{display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '15px', marginBottom: '20px'}}>
                            <div style={{background: '#f8f9fa', padding: '20px', borderRadius: '8px', border: '2px solid #e9ecef'}}>
                                <div style={{fontSize: '14px', color: '#6c757d', marginBottom: '5px'}}>{t('moderation.stats.total')}</div>
                                <div style={{fontSize: '32px', fontWeight: 'bold', color: '#495057'}}>{stats.total}</div>
                            </div>
                            <div style={{background: '#fff3cd', padding: '20px', borderRadius: '8px', border: '2px solid #ffc107'}}>
                                <div style={{fontSize: '14px', color: '#856404', marginBottom: '5px'}}>{t('moderation.stats.pending')}</div>
                                <div style={{fontSize: '32px', fontWeight: 'bold', color: '#856404'}}>{stats.pending}</div>
                            </div>
                            <div style={{background: '#d4edda', padding: '20px', borderRadius: '8px', border: '2px solid #28a745'}}>
                                <div style={{fontSize: '14px', color: '#155724', marginBottom: '5px'}}>{t('moderation.stats.approved')}</div>
                                <div style={{fontSize: '32px', fontWeight: 'bold', color: '#155724'}}>{stats.approved}</div>
                            </div>
                            <div style={{background: '#f8d7da', padding: '20px', borderRadius: '8px', border: '2px solid #dc3545'}}>
                                <div style={{fontSize: '14px', color: '#721c24', marginBottom: '5px'}}>{t('moderation.stats.rejected')}</div>
                                <div style={{fontSize: '32px', fontWeight: 'bold', color: '#721c24'}}>{stats.rejected}</div>
                            </div>
                        </div>
                
                <div style={{marginBottom: '20px', display: 'flex', gap: '10px'}}>
                    <Button
                        color={filter === 'all' ? 'accent' : 'default'}
                        label={t('moderation.filter.all')}
                        onClick={setFilterAll}
                    />
                    <Button
                        color={filter === 'pending' ? 'accent' : 'default'}
                        label={t('moderation.filter.pending')}
                        onClick={setFilterPending}
                    />
                    <Button
                        color={filter === 'approved' ? 'accent' : 'default'}
                        label={t('moderation.filter.approved')}
                        onClick={setFilterApproved}
                    />
                    <Button
                        color={filter === 'rejected' ? 'accent' : 'default'}
                        label={t('moderation.filter.rejected')}
                        onClick={setFilterRejected}
                    />
                </div>
                
                {loading && !data ? (
                    <div style={{textAlign: 'center', fontSize: '16px', color: '#6c757d', padding: '60px 20px', background: '#f8f9fa', borderRadius: '8px'}}>
                        {t('moderation.loading')}
                    </div>
                ) : error ? (
                    <p style={{color: 'red'}}>{t('moderation.error')}: {error.message}</p>
                ) : filteredComments.length === 0 ? (
                    <p>{t('moderation.noComments')}</p>
                ) : (
                    <div style={{display: 'flex', flexDirection: 'column', gap: '30px'}}>
                        {Object.entries(groupedComments).map(([postId, postComments]) => (
                            <div key={postId} style={{display: 'flex', flexDirection: 'column', gap: '15px'}}>
                                <h3 style={{fontSize: '18px', fontWeight: 'bold', color: '#495057', marginBottom: '10px', paddingBottom: '10px', borderBottom: '2px solid #dee2e6'}}>
                                    {postNames[postId] || postId}
                                </h3>
                                {postComments.map((comment, index) => {
                                    console.log(`Rendering comment ${index}:`, comment);
                                    try {
                                        return (
                                            <div key={comment.uuid} style={{background: 'white', borderRadius: '8px', boxShadow: '0 2px 4px rgba(0,0,0,0.1)', padding: '20px', flexShrink: 0, width: '100%'}}>
                                                <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '10px'}}>
                                                    <div style={{flex: 1}}>
                                                        {(() => {
                                                            const {date, time} = safeFormatDateTime(comment.created);
                                                            return (
                                                                <div style={{fontSize: '14px', color: '#6c757d', marginBottom: '5px'}}>
                                                                    <strong>{comment.author}</strong>
                                                                    {date && (
                                                                        <> • {date} {time}</>
                                                                    )}
                                                                </div>
                                                            );
                                                        })()}
                                                        <div style={{fontSize: '16px', color: '#212529', lineHeight: '1.5', whiteSpace: 'pre-wrap', wordBreak: 'break-word'}}>
                                                            {comment.body}
                                                        </div>
                                                    </div>
                                                    <span style={{
                                                        padding: '5px 10px',
                                                        borderRadius: '12px',
                                                        fontSize: '12px',
                                                        fontWeight: 'bold',
                                                        background: comment.status === 'approved' ? '#d4edda' : comment.status === 'rejected' ? '#f8d7da' : '#fff3cd',
                                                        color: comment.status === 'approved' ? '#155724' : comment.status === 'rejected' ? '#721c24' : '#856404',
                                                        marginLeft: '15px',
                                                        whiteSpace: 'nowrap'
                                                    }}>
                                                        {String(t(`moderation.status.${comment.status || 'pending'}`)).toUpperCase()}
                                                    </span>
                                                </div>
                                                <div style={{display: 'flex', gap: '10px', marginTop: '10px'}}>
                                                    {comment.status !== 'approved' && (
                                                        <Button
                                                            color="accent"
                                                            label={t('moderation.actions.approve')}
                                                            onClick={() => updateCommentStatus(comment.uuid, 'approved')}
                                                        />
                                                    )}
                                                    {comment.status !== 'rejected' && (
                                                        <Button
                                                            color="default"
                                                            label={t('moderation.actions.reject')}
                                                            onClick={() => updateCommentStatus(comment.uuid, 'rejected')}
                                                        />
                                                    )}
                                                    <Button
                                                        color="danger"
                                                        label={t('moderation.actions.delete')}
                                                        onClick={() => deleteComment(comment.uuid)}
                                                    />
                                                </div>
                                            </div>
                                        );
                                    } catch (err) {
                                        console.error(`ERROR rendering comment ${index}:`, err, comment);
                                        return (
                                            <div key={comment.uuid || `error-${index}`} style={{padding: '20px', background: '#fee', border: '2px solid red'}}>
                                                <h3>Error rendering comment {index}</h3>
                                                <pre>{err.toString()}</pre>
                                                <pre>{JSON.stringify(comment, null, 2)}</pre>
                                            </div>
                                        );
                                    }
                                })}
                            </div>
                        ))}
                    </div>
                )}
                    </div>
                )}
            </div>
        </div>
        );
        console.log('TRY: JSX render completed successfully');
        return jsx;
    } catch (err) {
        console.error('FATAL ERROR during JSX render:', err);
        console.error('Error stack:', err.stack);
        return (
            <>
                <Header title="Error" />
                <div style={{padding: '20px', color: 'red'}}>
                    <h2>Render Error</h2>
                    <pre>{err.toString()}</pre>
                    <pre>{err.stack}</pre>
                </div>
            </>
        );
    }
};

export default CommentModeration;
