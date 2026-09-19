import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { BrandMark } from './BrandMark'

describe('BrandMark', () => {
  it('renders exactly one official KBase brand image', () => {
    render(<MemoryRouter><BrandMark responsive /></MemoryRouter>)
    expect(screen.getByRole('img', { name: 'KBase' })).toHaveAttribute('src', '/assets/kbase/kbase-logo-web.png')
    expect(document.querySelectorAll('img')).toHaveLength(1)
    expect(document.querySelector('source[media="(max-width: 639px)"]')).toHaveAttribute('srcset', '/assets/kbase/kbase-icon.png')
    expect(screen.queryByText(/^K$/)).not.toBeInTheDocument()
  })
})
