export function Button({ children, ...props }) { return <button className="gold-button" {...props}>{children}</button>; }
export function BackTitle({ children }) { return <div className="page-title"><span aria-hidden="true">←</span><h1>{children}</h1></div>; }
export function State({ error, children }) { return <div className={error ? 'state error' : 'state'}>{children}</div>; }
export function Section({ title, children, className='' }) { return <section className={`panel ${className}`}><h2>{title}</h2>{children}</section>; }
